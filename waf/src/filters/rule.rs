use pingora_http::{RequestHeader, ResponseHeader};

pub fn check_waf_violations(path: &str) -> bool {
    path.contains("../") || path.contains("%2e%2e") || path.contains(".env")
}

pub fn handle_cors_preflight(req: &RequestHeader, resp: &mut ResponseHeader) {
    if let Some(origin) = req.headers.get("Origin") {
        if origin == "http://localhost:3001" {
            resp.insert_header("Access-Control-Allow-Origin", "http://localhost:3001")
                .unwrap();
        }
    }
    resp.insert_header(
        "Access-Control-Allow-Methods",
        "GET, POST, PUT, DELETE, OPTIONS",
    )
    .unwrap();
    resp.insert_header(
        "Access-Control-Allow-Headers",
        "Authorization, Content-Type, X-Target-Node",
    )
    .unwrap();
}

pub fn inject_security_headers(resp: &mut ResponseHeader) {
    resp.insert_header(
        "Content-Security-Policy",
        "default-src 'self'; frame-ancestors 'none';",
    )
    .unwrap();
    resp.insert_header("X-Content-Type-Options", "nosniff")
        .unwrap();
    resp.insert_header("X-Frame-Options", "DENY").unwrap();
    resp.insert_header("X-XSS-Protection", "1; mode=block")
        .unwrap();
    resp.insert_header(
        "Strict-Transport-Security",
        "max-age=31536000; includeSubDomains",
    )
    .unwrap();
    resp.insert_header("Cache-Control", "no-store, no-cache, must-revalidate")
        .unwrap();
    resp.insert_header("Pragma", "no-cache").unwrap();
}
