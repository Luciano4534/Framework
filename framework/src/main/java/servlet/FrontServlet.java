package servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import util.ControllerScanner;
import util.UrlMapping;
import util.UrlScanner;

/**
 * FrontServlet is the main controller/entry point of the framework.
 * On initialization, it scans controllers and compiles URL mapping tables.
 * For incoming requests, it routes paths to the corresponding controller methods.
 */
public class FrontServlet extends HttpServlet {

    private List<Class<?>> controllerClasses = new ArrayList<>();
    private HashMap<String, UrlMapping> mappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        // Read the 'controller-package' parameter declared in web.xml
        String packageName = getServletContext().getInitParameter("controller-package");

        if (packageName == null || packageName.trim().isEmpty()) {
            System.out.println("[FrontServlet] Warning: context-param 'controller-package' is not configured or empty.");
        } else {
            System.out.println("[FrontServlet] Initializing and scanning package: " + packageName);
            
            // Sprint 1: Scan package and get controllers list
            this.controllerClasses = ControllerScanner.findControllers(packageName.trim());
            
            // Print discovered controllers to the console (Sprint 1)
            System.out.println("\nController trouvé :");
            for (Class<?> clazz : this.controllerClasses) {
                System.out.println(clazz.getName());
            }
            System.out.println();

            // Sprint 2: Scan controllers to extract URL mappings
            try {
                this.mappings = UrlScanner.scan(this.controllerClasses);
                
                // Print URL mappings to the console (Sprint 2)
                System.out.println("--- URL MAPPINGS REGISTERED ---");
                for (Map.Entry<String, UrlMapping> entry : this.mappings.entrySet()) {
                    System.out.println("URL : " + entry.getKey());
                    System.out.println("Controller :");
                    System.out.println(entry.getValue().getClassName());
                    System.out.println("Méthode :");
                    System.out.println(entry.getValue().getMethodName());
                    System.out.println();
                }
                System.out.println("--------------------------------\n");
            } catch (Exception e) {
                System.err.println("[FrontServlet] Error scanning URL mappings: " + e.getMessage());
                throw new ServletException("Failed to scan URL mappings due to duplicate definitions.", e);
            }
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        // 1. Check if the requested path matches any registered mapping
        if (mappings.containsKey(path)) {
            UrlMapping mapping = mappings.get(path);

            // Print finding confirmation to console (Sprint 2 - Requirement 6)
            System.out.println("\n[FrontServlet] Request received for: " + path);
            System.out.println("Controller trouvé");
            System.out.println("Méthode trouvée");
            System.out.println();

            // Return a beautiful confirmation dashboard to the browser
            sendSuccessResponse(resp, path, mapping);
        } else {
            // 2. If the URL is unknown, return a 404 response with all available mappings (Sprint 2 - Requirement 7)
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            sendNotFoundResponse(resp, path);
        }
    }

    /**
     * Renders a premium success page confirming that the controller and method were matched successfully.
     */
    private void sendSuccessResponse(HttpServletResponse resp, String path, UrlMapping mapping) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset=\"UTF-8\">");
            out.println("<title>Mini MVC - Dispatch Success</title>");
            out.println("<link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap\" rel=\"stylesheet\">");
            out.println("<style>");
            out.println("    body {");
            out.println("        font-family: 'Outfit', sans-serif;");
            out.println("        background-color: #0f172a;");
            out.println("        color: #f8fafc;");
            out.println("        margin: 0;");
            out.println("        padding: 40px 20px;");
            out.println("        display: flex;");
            out.println("        justify-content: center;");
            out.println("        align-items: center;");
            out.println("        min-height: 100vh;");
            out.println("        box-sizing: border-box;");
            out.println("    }");
            out.println("    .card {");
            out.println("        background: rgba(30, 41, 59, 0.75);");
            out.println("        backdrop-filter: blur(12px);");
            out.println("        border: 1px solid rgba(56, 189, 248, 0.3);");
            out.println("        border-radius: 16px;");
            out.println("        padding: 40px;");
            out.println("        max-width: 600px;");
            out.println("        width: 100%;");
            out.println("        box-shadow: 0 10px 30px rgba(56, 189, 248, 0.1);");
            out.println("    }");
            out.println("    .status-badge {");
            out.println("        display: inline-block;");
            out.println("        background: rgba(16, 185, 129, 0.2);");
            out.println("        color: #10b981;");
            out.println("        border: 1px solid rgba(16, 185, 129, 0.4);");
            out.println("        border-radius: 9999px;");
            out.println("        padding: 4px 12px;");
            out.println("        font-size: 0.85rem;");
            out.println("        font-weight: 600;");
            out.println("        margin-bottom: 20px;");
            out.println("    }");
            out.println("    h1 {");
            out.println("        font-size: 1.8rem;");
            out.println("        font-weight: 700;");
            out.println("        margin-top: 0;");
            out.println("        margin-bottom: 8px;");
            out.println("        background: linear-gradient(135deg, #10b981, #3b82f6);");
            out.println("        -webkit-background-clip: text;");
            out.println("        -webkit-text-fill-color: transparent;");
            out.println("    }");
            out.println("    .subtitle {");
            out.println("        color: #94a3b8;");
            out.println("        font-size: 1rem;");
            out.println("        margin-bottom: 30px;");
            out.println("    }");
            out.println("    .mapping-detail {");
            out.println("        background: rgba(15, 23, 42, 0.4);");
            out.println("        border-radius: 8px;");
            out.println("        padding: 20px;");
            out.println("        border-left: 4px solid #3b82f6;");
            out.println("        margin-bottom: 20px;");
            out.println("    }");
            out.println("    .detail-row {");
            out.println("        margin-bottom: 12px;");
            out.println("    }");
            out.println("    .detail-row:last-child {");
            out.println("        margin-bottom: 0;");
            out.println("    }");
            out.println("    .label {");
            out.println("        font-size: 0.85rem;");
            out.println("        color: #64748b;");
            out.println("        text-transform: uppercase;");
            out.println("        letter-spacing: 0.05em;");
            out.println("        margin-bottom: 4px;");
            out.println("    }");
            out.println("    .value {");
            out.println("        font-family: monospace;");
            out.println("        font-size: 1rem;");
            out.println("        color: #f1f5f9;");
            out.println("    }");
            out.println("    .footer {");
            out.println("        margin-top: 30px;");
            out.println("        font-size: 0.85rem;");
            out.println("        color: #64748b;");
            out.println("        text-align: center;");
            out.println("    }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class=\"card\">");
            out.println("        <div class=\"status-badge\">Routage Validé</div>");
            out.println("        <h1>Routage Trouvé</h1>");
            out.println("        <p class=\"subtitle\">Sprint 2 - Association URL à une méthode</p>");
            out.println("        <div class=\"mapping-detail\">");
            out.println("            <div class=\"detail-row\">");
            out.println("                <div class=\"label\">URL Demandée</div>");
            out.println("                <div class=\"value\" style=\"color: #38bdf8;\">" + path + "</div>");
            out.println("            </div>");
            out.println("            <div class=\"detail-row\">");
            out.println("                <div class=\"label\">Controller class</div>");
            out.println("                <div class=\"value\">" + mapping.getClassName() + "</div>");
            out.println("            </div>");
            out.println("            <div class=\"detail-row\">");
            out.println("                <div class=\"label\">Méthode cible</div>");
            out.println("                <div class=\"value\" style=\"color: #818cf8;\">" + mapping.getMethodName() + "()</div>");
            out.println("            </div>");
            out.println("        </div>");
            out.println("        <div class=\"footer\">La méthode n'est pas encore invoquée (ce sera fait au Sprint 3).</div>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    /**
     * Renders a custom 404 page indicating the URL is unknown, and prints the list of available mappings.
     */
    private void sendNotFoundResponse(HttpServletResponse resp, String requestedPath) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset=\"UTF-8\">");
            out.println("<title>Mini MVC - URL Inconnue</title>");
            out.println("<link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap\" rel=\"stylesheet\">");
            out.println("<style>");
            out.println("    body {");
            out.println("        font-family: 'Outfit', sans-serif;");
            out.println("        background-color: #0f172a;");
            out.println("        color: #f8fafc;");
            out.println("        margin: 0;");
            out.println("        padding: 40px 20px;");
            out.println("        display: flex;");
            out.println("        justify-content: center;");
            out.println("        align-items: center;");
            out.println("        min-height: 100vh;");
            out.println("        box-sizing: border-box;");
            out.println("    }");
            out.println("    .card {");
            out.println("        background: rgba(30, 41, 59, 0.7);");
            out.println("        backdrop-filter: blur(12px);");
            out.println("        border: 1px solid rgba(239, 68, 68, 0.3);");
            out.println("        border-radius: 16px;");
            out.println("        padding: 40px;");
            out.println("        max-width: 600px;");
            out.println("        width: 100%;");
            out.println("        box-shadow: 0 10px 30px rgba(239, 68, 68, 0.1);");
            out.println("    }");
            out.println("    .error-badge {");
            out.println("        display: inline-block;");
            out.println("        background: rgba(239, 68, 68, 0.2);");
            out.println("        color: #ef4444;");
            out.println("        border: 1px solid rgba(239, 68, 68, 0.4);");
            out.println("        border-radius: 9999px;");
            out.println("        padding: 4px 12px;");
            out.println("        font-size: 0.85rem;");
            out.println("        font-weight: 600;");
            out.println("        margin-bottom: 20px;");
            out.println("    }");
            out.println("    h1 {");
            out.println("        font-size: 1.8rem;");
            out.println("        font-weight: 700;");
            out.println("        margin-top: 0;");
            out.println("        margin-bottom: 8px;");
            out.println("        background: linear-gradient(135deg, #ef4444, #f97316);");
            out.println("        -webkit-background-clip: text;");
            out.println("        -webkit-text-fill-color: transparent;");
            out.println("    }");
            out.println("    .subtitle {");
            out.println("        color: #94a3b8;");
            out.println("        font-size: 1rem;");
            out.println("        margin-bottom: 30px;");
            out.println("    }");
            out.println("    .unknown-url-box {");
            out.println("        background: rgba(239, 68, 68, 0.05);");
            out.println("        border-radius: 8px;");
            out.println("        padding: 16px;");
            out.println("        border: 1px dashed rgba(239, 68, 68, 0.3);");
            out.println("        font-family: monospace;");
            out.println("        font-size: 1rem;");
            out.println("        color: #ef4444;");
            out.println("        margin-bottom: 24px;");
            out.println("    }");
            out.println("    .section-title {");
            out.println("        font-size: 1rem;");
            out.println("        font-weight: 600;");
            out.println("        color: #cbd5e1;");
            out.println("        margin-bottom: 12px;");
            out.println("        border-bottom: 1px solid rgba(255, 255, 255, 0.1);");
            out.println("        padding-bottom: 6px;");
            out.println("    }");
            out.println("    ul {");
            out.println("        list-style: none;");
            out.println("        padding: 0;");
            out.println("        margin: 0;");
            out.println("    }");
            out.println("    li {");
            out.println("        background: rgba(255, 255, 255, 0.03);");
            out.println("        border-radius: 6px;");
            out.println("        padding: 10px 14px;");
            out.println("        margin-bottom: 8px;");
            out.println("        font-family: monospace;");
            out.println("        font-size: 0.9rem;");
            out.println("        color: #38bdf8;");
            out.println("        border-left: 3px solid #38bdf8;");
            out.println("    }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("    <div class=\"card\">");
            out.println("        <div class=\"error-badge\">404 Not Found</div>");
            out.println("        <h1>URL inconnue</h1>");
            out.println("        <p class=\"subtitle\">La ressource demandée n'est pas mappée dans le framework.</p>");
            out.println("        <div class=\"unknown-url-box\">");
            out.println("            URL inconnue : " + requestedPath);
            out.println("        </div>");
            out.println("        <div class=\"section-title\">URLs disponibles :</div>");
            out.println("        <ul>");
            if (mappings.isEmpty()) {
                out.println("            <li style=\"color: #94a3b8; border-left-color: #64748b;\">Aucune URL enregistrée</li>");
            } else {
                for (String url : mappings.keySet()) {
                    out.println("            <li>" + url + "</li>");
                }
            }
            out.println("        </ul>");
            out.println("    </div>");
            out.println("</body>");
            out.println("</html>");
        }
    }

    /**
     * Helper getter for controllers.
     */
    public List<Class<?>> getControllerClasses() {
        return controllerClasses;
    }

    /**
     * Helper getter for mappings.
     */
    public HashMap<String, UrlMapping> getMappings() {
        return mappings;
    }
}
