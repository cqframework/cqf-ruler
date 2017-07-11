package org.opencds.cqf.servlet;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.cqframework.cql.gen.cqlLexer;
import org.cqframework.cql.gen.cqlParser;
import org.cqframework.cql.tools.formatter.CqlFormatterVisitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Christopher Schuler on 7/6/2017.
 */
@WebServlet(name="format")
public class CqlFormatterServlet extends BaseServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.addHeader("Access-Control-Allow-Origin", "*");

        // validate that we are dealing with JSON or plain text
        if (!request.getContentType().equals("application/json") && !request.getContentType().equals("text/plain"))
        {
            throw new ServletException(String.format("Invalid content type %s. Please use application/json or text/plain.", request.getContentType()));
        }

        JSONParser parser = new JSONParser();
        JSONObject json;
        try {
            json = (JSONObject) parser.parse(request.getReader());
        } catch (ParseException e) {
            throw new ServletException("Error parsing JSON request: " + e.getMessage());
        }

        String code = (String) json.get("code");

        ANTLRInputStream input = new ANTLRInputStream(code);
        cqlLexer lexer = new cqlLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        cqlParser cqlParser = new cqlParser(tokens);
        cqlParser.setBuildParseTree(true);
        ParserRuleContext tree = cqlParser.library();
        CqlFormatterVisitor formatter = new CqlFormatterVisitor();
        String output = (String) formatter.visit(tree);

        JSONArray result = new JSONArray();
        JSONObject element = new JSONObject();
        element.put("formatted-cql", output);
        result.add(element);

        response.getWriter().println(result.toJSONString());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        throw new ServletException("This servlet is not configured to handle GET requests.");
    }
}
