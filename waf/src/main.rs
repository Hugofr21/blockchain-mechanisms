mod filters;
mod proxy;

use async_trait::async_trait;
use pingora_core::Result;
use pingora_core::server::Server;
use pingora_core::upstreams::peer::HttpPeer;
use pingora_http::{RequestHeader, ResponseHeader};
use pingora_proxy::{ProxyHttp, Session};

use filters::rules::{check_waf_violations, handle_cors_preflight, inject_security_headers};
use proxy::resource::{resolve_upstream_peer, rewrite_upstream_uri};

pub struct EdgeWafGateway;

#[async_trait]
impl ProxyHttp for EdgeWafGateway {
    type CTX = ();

    fn new_ctx(&self) -> () {
        ()
    }

    async fn request_filter(&self, session: &mut Session, _ctx: &mut Self::CTX) -> Result<bool> {
        let path = session.req_header().uri.path();
        if check_waf_violations(path) {
            let resp = ResponseHeader::build(403, Some(3)).unwrap();
            session.write_response_header(Box::new(resp), false).await?;
            session
                .write_response_body(Some("Access Denied.".into()), true)
                .await?;

            return Ok(true);
        }

        if session.req_header().method.as_str() == "OPTIONS" {
            let mut resp = ResponseHeader::build(204, None).unwrap();
            handle_cors_preflight(session.req_header(), &mut resp);

            session.write_response_header(Box::new(resp), true).await?;

            return Ok(true);
        }

        Ok(false)
    }

    async fn upstream_peer(
        &self,
        session: &mut Session,
        _ctx: &mut Self::CTX,
    ) -> Result<Box<HttpPeer>> {
        let peer = resolve_upstream_peer(session.req_header().uri.path());
        Ok(Box::new(peer))
    }

    async fn upstream_request_filter(
        &self,
        _session: &mut Session,
        upstream_request: &mut RequestHeader,
        _ctx: &mut Self::CTX,
    ) -> Result<()> {
        rewrite_upstream_uri(upstream_request);
        Ok(())
    }

    async fn response_filter(
        &self,
        session: &mut Session,
        upstream_response: &mut ResponseHeader,
        _ctx: &mut Self::CTX,
    ) -> Result<()> {
        upstream_response.remove_header("Access-Control-Allow-Origin");
        upstream_response.remove_header("Access-Control-Allow-Methods");
        upstream_response.remove_header("Access-Control-Allow-Headers");
        handle_cors_preflight(session.req_header(), upstream_response);
        inject_security_headers(upstream_response);
        Ok(())
    }
}

fn main() {
    let mut server = Server::new(None).unwrap();
    server.bootstrap();

    let mut edge_proxy = pingora_proxy::http_proxy_service(&server.configuration, EdgeWafGateway);
    edge_proxy.add_tcp("0.0.0.0:80");

    server.add_service(edge_proxy);
    server.run_forever();
}
