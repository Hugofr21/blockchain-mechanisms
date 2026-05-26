use async_trait::async_trait;
use lazy_static::lazy_static;
use pingora_core::server::Server;
use pingora_core::upstreams::peer::HttpPeer;
use pingora_core::Result;
use pingora_http::{RequestHeader, ResponseHeader};
use pingora_proxy::{ProxyHttp, Session};
use regex::Regex;

fn main() {
    println!("Hello, world!");
}
