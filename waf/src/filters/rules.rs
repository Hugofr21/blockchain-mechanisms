use pingora_http::{RequestHeader, ResponseHeader};

pub fn check_waf_violations(uri: &http::Uri) -> bool {
    let path = uri.path();
    let query = uri.query().unwrap_or("");

    // 1. Vetores de Traversal de Diretórios e Exposição de Arquivos Sensíveis
    if path.contains("../") || path.contains("%2e%2e") || path.contains(".env") {
        return true;
    }
    // 2. Vetores de Injeção SQL e XSS
    if query.contains("<script>")
        || query.contains("%3Cscript%3E")
        || query.contains("SELECT")
        || query.contains("UNION")
    {
        return true;
    }

    false
}

pub fn handle_cors_preflight(req: &RequestHeader, resp: &mut ResponseHeader) {
    if let Some(origin) = req.headers.get("Origin") {
        if let Ok(origin_str) = origin.to_str() {
            if origin_str == "http://localhost:3001" || origin_str == "http://127.0.0.1:3001" {
                let _ = resp.insert_header("Access-Control-Allow-Origin", origin_str);
            }
        }
    }

    let _ = resp.insert_header(
        "Access-Control-Allow-Methods",
        "GET, POST, PUT, DELETE, OPTIONS",
    );
    let _ = resp.insert_header(
        "Access-Control-Allow-Headers",
        "Authorization, Content-Type, X-Target-Node, Accept",
    );
}

pub fn inject_security_headers(resp: &mut ResponseHeader) {
    let _ = resp.remove_header("Server");
    let _ = resp.remove_header("X-Powered-By");

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
    resp.insert_header(
        "Cache-Control",
        "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0",
    )
    .unwrap();
    resp.insert_header("Pragma", "no-cache").unwrap();
}
