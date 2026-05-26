use lazy_static::lazy_static;
use pingora_core::upstreams::peer::HttpPeer;
use pingora_http::RequestHeader;
use regex::Regex;

lazy_static! {
    static ref PEER_ROUTE_REGEX: Regex = Regex::new(r"^/api/peer-(\d+)(/.*)?").unwrap();
    static ref BOOTSTRAP_ROUTE_REGEX: Regex = Regex::new(r"^/api/bootstrap(/.*)?").unwrap();
    static ref AGENT_ROUTE: Regex = Regex::new(r"^/agent(/.*)?").unwrap();
    static ref KEYCLOAK_ROUTE: Regex = Regex::new(r"^/auth(/.*)?").unwrap();
}

pub fn resolve_upstream_peer(path: &str) -> HttpPeer {
    // 1. Interceção e desvio para o Agente de Infraestrutura
    if AGENT_ROUTE.is_match(path) {
        return HttpPeer::new("172.23.0.5:4000", false, "".to_string());
    }

    // 2. Interceção e desvio para o Servidor de Identidade (Keycloak)
    if KEYCLOAK_ROUTE.is_match(path) {
        return HttpPeer::new("172.23.0.80:8080", false, "".to_string());
    }

    // 3. Fallback restrito à malha Kademlia gerida pelo proxy interno Nginx
    HttpPeer::new("172.23.0.99:80", false, "".to_string())
}

pub fn rewrite_upstream_uri(upstream_request: &mut RequestHeader) {
    let path = upstream_request.uri.path();
    let query = upstream_request
        .uri
        .query()
        .map(|q| format!("?{}", q))
        .unwrap_or_default();

    if let Some(caps) = AGENT_ROUTE.captures(path) {
        let new_path = caps.get(1).map_or("/", |m| m.as_str());
        let full_uri = format!("{}{}", new_path, query);
        if let Ok(new_uri) = full_uri.parse::<http::Uri>() {
            upstream_request.set_uri(new_uri);
        }
    }
}
