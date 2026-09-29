# Rate Limiter Algorithms

Implementações dos seis algoritmos clássicos de rate limiting em Java 21, com a mesma interface, testes determinísticos, uma API Spring Boot que aplica cada um deles e um playground visual que coloca todos lado a lado sob o mesmo tráfego.

| Algoritmo | Estado por chave | Rajadas | Precisão | Quando usar |
|---|---|---|---|---|
| Fixed Window Counter | 2 longs | Até 2x o limite na virada da janela | Baixa | Cotas simples (ex.: 1000 req/dia) |
| Sliding Window Log | O(limite) timestamps | Não | Exata | Limites baixos que exigem precisão |
| Sliding Window Counter | 3 longs | Não | Aproximada | Padrão de mercado para APIs (Cloudflare) |
| Token Bucket | 1 double + 1 long | Sim, até a capacidade | Exata | APIs que toleram picos curtos (AWS, Stripe) |
| Leaky Bucket (fila) | 1 long | Absorvidas com atraso | Exata | Suavizar tráfego para um backend frágil (nginx `limit_req`) |
| GCRA | 1 long | Sim, até o limite | Exata | Token bucket distribuído com uma chave no Redis |

## Como rodar

Requisitos: JDK 21+.

```bash
./mvnw spring-boot:run
```

Abra http://localhost:8080 para o playground.

Ou com Docker:

```bash
docker build -t rate-limiter-algorithms .
docker run -p 8080:8080 rate-limiter-algorithms
```

Testes:

```bash
./mvnw test
```

## Playground

A página inicial chama `/api/simulations`, que roda os seis algoritmos contra o mesmo tráfego sintético usando um relógio simulado. Cada algoritmo vira uma linha do tempo: traços verdes são requisições aceitas, amarelos são aceitas com atraso (posicionados no momento em que seriam executadas) e vermelhos são rejeitadas.

Padrões de tráfego disponíveis:

| Padrão | Descrição |
|---|---|
| `window-boundary` | `limite` requisições nos últimos 10% de uma janela e `limite` nos primeiros 10% da seguinte |
| `burst` | Rajada instantânea de 2x o limite a cada 2 períodos |
| `steady` | Fluxo constante a 2x o limite |
| `random` | Chegadas de Poisson a 1.5x o limite, com seed fixa |

Resultado com limite 10 por segundo durante 6 segundos (`pico` é o maior número de requisições executadas em qualquer intervalo de 1 segundo):

| Algoritmo | window-boundary | steady | random |
|---|---|---|---|
| Fixed Window | 60/60, **pico 20** | 60/120, pico 10 | 60/93, pico 15 |
| Sliding Window Log | 30/60, pico 10 | 60/120, pico 10 | 58/93, pico 10 |
| Sliding Window Counter | 30/60, pico 10 | 55/120, pico 10 | 54/93, pico 10 |
| Token Bucket | 33/60, pico 11 | 69/120, pico 19 | 69/93, pico 17 |
| Leaky Bucket | 33/60, pico 10 | 69/120, pico 10 | 69/93, pico 10 |
| GCRA | 33/60, pico 11 | 69/120, pico 19 | 69/93, pico 17 |

O que a tabela mostra:

- O Fixed Window aceita todas as 60 requisições do cenário `window-boundary`, deixando passar 20 requisições em 200 ms com limite 10/s.
- Token Bucket e GCRA aceitam mais tráfego porque a taxa média é respeitada, mas o balde cheio somado ao reabastecimento permite quase 2x o limite num único segundo. É uma escolha consciente: o limite define a taxa sustentada e a capacidade define a rajada.
- O Leaky Bucket aceita o mesmo volume que o Token Bucket, mas nunca passa de 10 execuções por segundo. O custo é latência: cada requisição espera em média centenas de milissegundos na fila.
- O Sliding Window Counter é conservador: como estima a janela anterior como uniformemente distribuída, às vezes rejeita requisições que o log exato aceitaria.

## Os algoritmos

Todos recebem um `RateLimitConfig(limit, period)` e implementam:

```java
public interface RateLimiter {
    RateLimitDecision tryAcquire(String key);
    Algorithm algorithm();
}
```

`RateLimitDecision` informa se a requisição foi aceita, quantas ainda restam, quanto esperar antes de tentar de novo (`retryAfter`) e, no caso do Leaky Bucket, quanto tempo a requisição deve aguardar antes de executar (`delay`).

### Fixed Window Counter

[`FixedWindowRateLimiter`](src/main/java/br/com/diegobraun/ratelimiter/algorithm/FixedWindowRateLimiter.java)

O tempo é dividido em janelas fixas (`[0s, 1s)`, `[1s, 2s)`, ...). Cada chave guarda o início da janela atual e um contador. Se a requisição cai numa janela nova, o contador zera.

```
janela 1              janela 2
|..........xxxxxxxxxx|xxxxxxxxxx..........|
            10 req    10 req
           └── 20 requisições em ~200 ms ──┘
```

É o mais simples e barato, e pode ser implementado com um único `INCR` + `EXPIRE` no Redis. O problema é a borda: um cliente pode gastar o limite no fim de uma janela e de novo no começo da próxima.

### Sliding Window Log

[`SlidingWindowLogRateLimiter`](src/main/java/br/com/diegobraun/ratelimiter/algorithm/SlidingWindowLogRateLimiter.java)

Guarda o timestamp de cada requisição aceita numa fila. A cada nova requisição, descarta os timestamps mais antigos que `agora - período` e aceita se ainda houver menos que `limite` na fila. O `retryAfter` é exato: o momento em que o timestamp mais antigo sai da janela.

É exato, mas usa memória proporcional ao limite. Com limite de 10.000 por hora e um milhão de clientes, isso vira bilhões de timestamps. No Redis costuma ser feito com um sorted set (`ZADD` + `ZREMRANGEBYSCORE` + `ZCARD`).

### Sliding Window Counter

[`SlidingWindowCounterRateLimiter`](src/main/java/br/com/diegobraun/ratelimiter/algorithm/SlidingWindowCounterRateLimiter.java)

Combina o custo do Fixed Window com quase toda a precisão do log. Guarda o contador da janela atual e da anterior e estima quantas requisições existem na janela deslizante:

```
estimativa = anterior × (1 − fração decorrida da janela atual) + atual
```

Exemplo com limite 10: a janela anterior teve 10 requisições e estamos a 30% da atual. A estimativa é `10 × 0.7 + 0 = 7`, então restam 3 requisições. A aproximação assume que as requisições da janela anterior estavam distribuídas de forma uniforme.

### Token Bucket

[`TokenBucketRateLimiter`](src/main/java/br/com/diegobraun/ratelimiter/algorithm/TokenBucketRateLimiter.java)

Um balde com capacidade `limite` começa cheio e é reabastecido a `limite / período` fichas por unidade de tempo. Cada requisição consome uma ficha. O reabastecimento é calculado de forma preguiçosa, só quando chega uma requisição, a partir do tempo decorrido desde a última:

```
fichas = min(capacidade, fichas + decorrido × taxa)
```

Permite rajadas até a capacidade e depois limita à taxa de reabastecimento. Taxa e capacidade são independentes em implementações reais. Aqui ambas derivam do mesmo `limite` para comparar os algoritmos com um único parâmetro.

### Leaky Bucket (fila)

[`LeakyBucketRateLimiter`](src/main/java/br/com/diegobraun/ratelimiter/algorithm/LeakyBucketRateLimiter.java)

Requisições entram numa fila de tamanho `limite` que "vaza" a uma taxa constante, uma requisição a cada `período / limite`. Em vez de rejeitar uma rajada, o algoritmo aceita e atrasa cada requisição até o seu slot de saída. Só rejeita quando a fila está cheia.

A implementação não mantém a fila em memória, só o instante do próximo slot livre. O atraso de uma requisição é `próximoSlot − agora`, e o número de requisições na frente dela é `⌈atraso / intervalo⌉`.

Isso é *traffic shaping*, não *policing*: o backend recebe um fluxo perfeitamente uniforme. Na API de demonstração, o interceptor segura a requisição pelo tempo indicado em `delay` (com virtual threads, isso não bloqueia uma thread de plataforma).

Existe outra variante, o "leaky bucket as a meter", que rejeita em vez de atrasar. Ela é matematicamente equivalente ao Token Bucket e ao GCRA, por isso não foi implementada separadamente.

### GCRA (Generic Cell Rate Algorithm)

[`GcraRateLimiter`](src/main/java/br/com/diegobraun/ratelimiter/algorithm/GcraRateLimiter.java)

Vem das redes ATM e produz as mesmas decisões que o Token Bucket, mas guarda um único valor por chave: o TAT (*theoretical arrival time*), o instante em que a próxima requisição chegaria se o cliente enviasse exatamente na taxa permitida.

```
intervalo = período / limite
novoTat   = max(tat, agora) + intervalo
liberaEm  = novoTat − intervalo × limite

se agora < liberaEm: rejeita, retryAfter = liberaEm − agora
senão:               aceita, tat = novoTat
```

Por não depender de reabastecimento nem de ponto flutuante, é o algoritmo preferido para rate limiting distribuído: o estado inteiro cabe numa chave do Redis e é atualizado por um script Lua atômico. É o que o módulo [redis-cell](https://github.com/brandur/redis-cell) e a biblioteca [throttled](https://github.com/throttled/throttled) usam.

## API

A configuração padrão é 5 requisições a cada 10 segundos, ajustável em [`application.yml`](src/main/resources/application.yml):

```yaml
rate-limit:
  limit: 5
  period: 10s
```

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/limited/{algorithm}` | Endpoint protegido pelo algoritmo escolhido |
| `GET` | `/api/algorithms` | Lista os algoritmos |
| `GET` | `/api/patterns` | Lista os padrões de tráfego |
| `GET` | `/api/simulations` | Roda a simulação (`algorithm`, `pattern`, `limit`, `periodMs`, `periods`, `seed`) |

Valores de `{algorithm}`: `fixed-window`, `sliding-window-log`, `sliding-window-counter`, `token-bucket`, `leaky-bucket`, `gcra`.

O cliente é identificado pelo header `X-Client-Id` ou, na falta dele, pelo IP.

```bash
for i in $(seq 1 7); do
  curl -s -o /dev/null -w "%{http_code} " -H "X-Client-Id: diego" localhost:8080/api/limited/gcra
done
# 200 200 200 200 200 429 429
```

Resposta quando o limite é excedido:

```
HTTP/1.1 429
X-RateLimit-Algorithm: gcra
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 0
Retry-After: 2

{"error":"Too Many Requests","algorithm":"gcra","retryAfterMs":1924}
```

Com `leaky-bucket`, as requisições em excesso não recebem 429 enquanto houver espaço na fila. Elas demoram mais para responder e trazem o header `X-RateLimit-Delay-Ms`.

## Estrutura

```
src/main/java/br/com/diegobraun/ratelimiter
├── core/         RateLimiter, RateLimitDecision, RateLimitConfig, Ticker, KeyedStateRateLimiter
├── algorithm/    uma classe por algoritmo
├── simulation/   gerador de tráfego e simulador com relógio controlado
└── web/          interceptor, controllers e registro dos limiters
```

Decisões de design:

- **Relógio injetável.** Todo algoritmo lê o tempo de um `Ticker`. Em produção é `System.nanoTime()` (monotônico, imune a ajustes de relógio). Nos testes e no simulador é um `ManualTicker` avançado manualmente, o que torna os testes determinísticos e sem `Thread.sleep`.
- **Estado por chave com lock fino.** `KeyedStateRateLimiter` guarda o estado de cada chave num `ConcurrentHashMap` e sincroniza apenas no objeto daquela chave. Clientes diferentes nunca disputam o mesmo lock.
- **Teste de contrato.** [`RateLimiterContractTest`](src/test/java/br/com/diegobraun/ratelimiter/algorithm/RateLimiterContractTest.java) roda as mesmas propriedades contra os seis algoritmos: rajada aceita exatamente `limite`, chaves isoladas, `retryAfter` correto, e 1000 requisições concorrentes em 16 threads resultam em exatamente `limite` aceitas.

## Limitações e próximos passos

- O estado vive em memória de um único processo. Com várias instâncias atrás de um load balancer, cada uma aplicaria o limite separadamente. O passo natural é uma implementação com Redis e scripts Lua (GCRA e Sliding Window Counter são os mais adequados).
- Chaves inativas nunca são removidas do mapa. Em produção seria necessário expirar estados ociosos (por exemplo com Caffeine e `expireAfterAccess`).
- `tryAcquire` consome uma unidade por chamada. Custos variáveis por requisição (ex.: endpoints mais caros consumindo mais fichas) não foram modelados.

## Licença

[MIT](LICENSE)
