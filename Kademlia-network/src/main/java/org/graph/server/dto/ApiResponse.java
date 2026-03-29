package org.graph.server.dto;


public record ApiResponse(String status, String message, int port) {}