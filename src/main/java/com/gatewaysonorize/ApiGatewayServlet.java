
package com.gatewaysonorize;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
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
            "users", "http://localhost:8080/users"
    );


    protected void handleRequest(HttpServletRequest request, HttpServletResponse response, String method) throws IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 1. Leer JSON del cuerpo si existe
        JsonObject data = null;
        try (JsonReader jsonReader = Json.createReader(request.getInputStream())) {
            data = jsonReader.readObject();
            // Puedes usar los campos si los necesitas, por ejemplo:
            String email = data.containsKey("email") ? data.getString("email") : "no email";
            System.out.println("Email recibido: " + email);
        } catch (Exception e) {
            // Si no hay JSON válido, continuar normalmente
            System.out.println("No se pudo leer JSON (puede que no haya cuerpo JSON)");
        }

        // 2. Lógica original que ya tenías
        String path = request.getPathInfo();
        String cleanPath = path != null ? path.replace("/", "") : "";

        String serviceUrl = servicesMap.get(cleanPath);

        String res;
        if (serviceUrl != null) {
            res = "Service URL is " + serviceUrl;
        } else {
            res = "Service path not found for value: " + cleanPath;
        }

        // 3. Respuesta
        try (PrintWriter out = response.getWriter()) {
            out.print(Json.createObjectBuilder()
                         .add("message", res)
                         .build()
                         .toString());
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
