use lazy_static::lazy_static;
use pingora_core::upstreams::peer::HttpPeer;
use pingora_http::RequestHeader;
use regex::Regex;

lazy_static! {
    static ref PEER_ROUTE_REGEX: Regex = Regex::new(r"^/api/peer-(\d+)(/.*)?").unwrap();
    static ref BOOTSTRAP_ROUTE_REGEX: Regex = Regex::new(r"^/api/bootstrap(/.*)?").unwrap();
}

pub fn resolve_upstream_peer(path: &str) -> HttpPeer {
    if BOOTSTRAP_ROUTE_REGEX.is_match(path) {
        return HttpPeer::new("bootstrap-node:8080", false, "".to_string());
    }

    if let Some(caps) = PEER_ROUTE_REGEX.captures(path) {
        let port = caps.get(1).map_or("8080", |m| m.as_str());
        let target_host = format!("peer-{}:{}", port, port);
        return HttpPeer::new(target_host, false, "".to_string());
    }
    HttpPeer::new("127.0.0.1:9999", false, "".to_string())
}

pub fn rewrite_upstream_uri(upstream_request: &mut RequestHeader) {
    let path = upstream_request.uri.path();

    let query = upstream_request
        .uri
        .query()
        .map(|q| format!("?{}", q))
        .unwrap_or_default();

    if let Some(caps) = BOOTSTRAP_ROUTE_REGEX.captures(path) {
        let new_path = caps.get(1).map_or("/", |m| m.as_str());
        let full_uri = format!("{}{}", new_path, query);

        if let Ok(new_uri) = full_uri.parse::<http::Uri>() {
            upstream_request.set_uri(new_uri);
        }
    } else if let Some(caps) = PEER_ROUTE_REGEX.captures(path) {
        let new_path = caps.get(2).map_or("/", |m| m.as_str());
        let full_uri = format!("{}{}", new_path, query);

        if let Ok(new_uri) = full_uri.parse::<http::Uri>() {
            upstream_request.set_uri(new_uri);
        }
    }
}
