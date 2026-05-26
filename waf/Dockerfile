FROM rust:bookworm AS builder
WORKDIR /app
RUN apt-get update && apt-get install -y cmake pkg-config libssl-dev clang
COPY . .
RUN cargo build --release

FROM debian:bookworm-slim
WORKDIR /app
RUN apt-get update && apt-get install -y ca-certificates && rm -rf /var/lib/apt/lists/*
RUN useradd -m -s /bin/bash waf_user
USER waf_user
COPY --from=builder /app/target/release/waf /usr/local/bin/waf
EXPOSE 8060
CMD ["waf"]