
package com.gatewaysonorize;

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;


@WebServlet(name = "ApiGatewayServlet", urlPatterns = {"/api/*"})
public class ApiGatewayServlet extends HttpServlet {
    
    
    private final Map<String, String> servicesMap = Map.of(
            "users", "http://localhost:8081/users"
    );


    protected void handleRequest(HttpServletRequest request, HttpServletResponse response, String method) throws IOException {
        
        String path = request.getPathInfo();
        String cleanPath = path.replace("/", "");
        
        String serviceUrl = servicesMap.get(cleanPath);
        
        String res = null;
                
        if ( serviceUrl != null) res = "Service URL is " + serviceUrl;
        else res = "Service path not found for value: " + cleanPath;
        
        
        try (PrintWriter out = response.getWriter()) {
            out.print(res);
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
        return "Api gateway for Sonorize";
    }// </editor-fold>

}
