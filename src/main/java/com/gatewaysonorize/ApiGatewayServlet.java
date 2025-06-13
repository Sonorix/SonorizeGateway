package com.gatewaysonorize;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

/**
 * Handles all incoming requests and forwards them to appropriate microservices
 */
@WebServlet(name = "ApiGatewayServlet", urlPatterns = {"/api/*"})
public class ApiGatewayServlet extends HttpServlet {
    
    // Define service mappings with base URLs
    private final Map<String, String> servicesMap = Map.of(
            "users", "http://localhost:8081/users-ms",
            "contracts", "http://localhost:8081/contratacion-transaccion"
    );
    
    // Define endpoint mappings with their respective services
    private final Map<String, String> endpointMappings = Map.of(
            "login", "users",
            "register", "users",
            "contract", "contracts",
            "payment", "contracts",
            "eventos", "contracts"
    );

    /**
     * Handles all incoming requests and forwards them to the appropriate microservice
     */
    protected void handleRequest(HttpServletRequest request, HttpServletResponse response, String method) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try {
            // Parse the requested path
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/")) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid API endpoint");
                return;
            }
            
            // Remove leading slash and get the endpoint
            String endpoint = pathInfo.substring(1);
            
            // Check if endpoint is valid
            if (!endpointMappings.containsKey(endpoint)) {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, "Endpoint not found: " + endpoint);
                return;
            }
            
            // Get the service for this endpoint
            String serviceName = endpointMappings.get(endpoint);
            String serviceUrl = servicesMap.get(serviceName);
            
            if (serviceUrl == null) {
                sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Service configuration error");
                return;
            }
            
            // Build the target URL for the microservice
            String targetUrl = serviceUrl + "/" + endpoint;
            
            // Log the request (for debugging)
            System.out.println("[Gateway] Forwarding " + method + " request to: " + targetUrl);
            
            // Forward the request to the microservice
            forwardRequest(request, response, targetUrl, method);
            
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Gateway error: " + e.getMessage());
        }
    }
    
    /**
     * Forwards the request to the specified URL using HttpClient5
     */
    private void forwardRequest(HttpServletRequest request, HttpServletResponse response, String targetUrl, String method) throws IOException, ParseException {
        // Create HttpClient instance
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Create appropriate request method based on the incoming request
            HttpUriRequest httpRequest;
            
            switch (method) {
                case "GET":
                    httpRequest = new HttpGet(targetUrl);
                    break;
                case "POST":
                    httpRequest = new HttpPost(targetUrl);
                    break;
                case "PUT":
                    httpRequest = new HttpPut(targetUrl);
                    break;
                case "DELETE":
                    httpRequest = new HttpDelete(targetUrl);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }
            
            // Copy relevant headers from the client request to our request
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    
                    // Skip certain headers to avoid conflicts
                    if (!headerName.equalsIgnoreCase("host") && 
                        !headerName.equalsIgnoreCase("content-length")) {
                        httpRequest.setHeader(headerName, headerValue);
                    }
                }
            }
            
            // Set JSON content type if not already set
            if (!httpRequest.containsHeader("Content-Type")) {
                httpRequest.setHeader("Content-Type", "application/json");
            }
            
            // Add request body for POST and PUT requests
            if ("POST".equals(method) || "PUT".equals(method)) {
                // Read the request body
                String requestBody = readInputStreamAsString(request.getInputStream());
                
                // Log request for debugging
                System.out.println("[Gateway] Request body: " + requestBody);
                
                // Set the entity to our request
                if (httpRequest instanceof HttpPost) {
                    ((HttpPost) httpRequest).setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
                } else if (httpRequest instanceof HttpPut) {
                    ((HttpPut) httpRequest).setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
                }
            }
            
            // Execute the request
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
                // Copy status code
                response.setStatus(httpResponse.getCode());
                
                // Copy headers
                for (Header header : httpResponse.getHeaders()) {
                    response.addHeader(header.getName(), header.getValue());
                }
                
                // Copy response entity (if any)
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    // Read and log the response body
                    String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    System.out.println("[Gateway] Response body: " + responseBody);
                    
                    // Forward the response to the client
                    try (PrintWriter out = response.getWriter()) {
                        out.write(responseBody);
                        out.flush();
                    }
                }
            }
        }
    }
    
    /**
     * Utility method to read an input stream into a String
     */
    private String readInputStreamAsString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }
    
    /**
     * Sends an error response
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        
        JsonObject error = Json.createObjectBuilder()
                .add("error", message)
                .build();
        
        try (PrintWriter out = response.getWriter()) {
            out.print(error.toString());
            out.flush();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response, "POST");
    }
    
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response, "PUT");
    }
    
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleRequest(request, response, "DELETE");
    }

    @Override
    public String getServletInfo() {
        return "API Gateway for Sonorize";
    }
}
