package flows;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class TraversalDemo {

    static Set<Node> visited = new HashSet<>();
    private static final Object STEP_LOCK = new Object();
    private static volatile boolean nextStep = false;

    static Viewer viewer;

    public static void main(String[] args) throws Exception {

        System.setProperty("org.graphstream.ui", "swing");

        Graph graph = new SingleGraph("DFS Demo");

        graph.setAttribute("ui.stylesheet", STYLE);
        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");

        /* addNode("A");
        addNode("B");
        addNode("C");
        addNode("D");
        addNode("E");
        addNode("F");

        addEdge("A", "B", 10);
        addEdge("A", "C", 5);
        addEdge("B", "D", 5);
        addEdge("B", "E", 8);
        addEdge("C", "E", 4);
        addEdge("E", "F", 6); */

        // Train graph
        addNode(graph, "A"); // Paris
        addNode(graph, "B"); // Calais
        addNode(graph, "C"); // Ghent
        addNode(graph, "D"); // Brussels
        addNode(graph, "E"); // Antwerp
        addNode(graph, "F"); // Rotterdam
        addNode(graph, "G"); // Amsterdam

        graph.setAttribute("source", graph.getNode("A"));
        graph.setAttribute("sink", graph.getNode(String.valueOf((char) (graph.getNode("A").getId().charAt(0) + graph.getNodeCount() - 1))));

        addEdge(graph, "A", "B", 8);
        addEdge(graph, "A", "C", 9);
        addEdge(graph, "A", "D", 5);
        addEdge(graph, "B", "E", 6);
        addEdge(graph, "C", "D", 7);
        addEdge(graph, "C", "F", 5);
        addEdge(graph, "D", "B", 1);
        addEdge(graph, "D", "E", 2);
        addEdge(graph, "D", "F", 6);
        addEdge(graph, "E", "G", 11);
        addEdge(graph, "F", "E", 4);
        addEdge(graph, "F", "G", 13);

        viewer = graph.display();
        JComponent comp = (JComponent) viewer.getDefaultView();

        InputMap im = comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = comp.getActionMap();

        AbstractAction nextAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (STEP_LOCK) {
                    nextStep = true;
                    STEP_LOCK.notifyAll();
                }
            }
        };

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "next");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "next");
        am.put("next", nextAction);

        // edmondsKarpBFH1(graph);
        // edmondsKarpBFH2(graph);
        // edmondsKarp(graph);
        //new Thread(() -> edmondsKarp(graph)).start();
        SwingUtilities.invokeLater(() -> {
            new Thread(() -> edmondsKarp(graph)).start();
        });
        
        // Thread.sleep(1000);
        // dfs(graph.getNode("A"));
    }

    static void dfs(Node node) throws Exception {
        visited.add(node);

        node.setAttribute("ui.class", "current");
        Thread.sleep(700);

        for (Edge edge : node.leavingEdges().toList()) {

            Node next = edge.getTargetNode();

            if (!visited.contains(next)) {

                edge.setAttribute("ui.class", "active");
                Thread.sleep(500);

                dfs(next);
            }
        }

        node.setAttribute("ui.class", "visited");
    }

    static Node addNode(Graph graph, String id) {
        Node n = graph.addNode(id);
        n.setAttribute("ui.label", id);

        return n;
    }

    static void addEdge(Graph graph, String from, String to, int capacity) {
        Edge e = graph.addEdge(from + to, from, to, true); // true = directed
        e.setAttribute("ui.label", capacity);
        e.setAttribute("capacity", capacity);
    }

    static void setFlow(Edge a, double flow) {
        a.setAttribute("flow", flow);
        a.setAttribute("ui.label", flow + "/" + a.getAttribute("capacity"));
    }

    static void edmondsKarpBFH1(Graph graph) {
            /* Counter example:

             a
            / \
            d  b
            \ / \
             c   e
              \ /
               f
            
            Consider the above graph with all arcs pointing down, each with capacity 1.
            Then sending one unit of flow through {a, b, c, f} yields the following graph with no more available source-sink paths:

             a
            / 
            d  b
            \   \
             c   e
                /
               f

            */

        for (Edge a : graph.edges().toList()) {
            setFlow(a, 0);
            a.setAttribute("Delta", Double.POSITIVE_INFINITY);
        }

        Map<Node, Node> pred;
        do {
            pred = breadthTraversal(graph); // this uses the oriented graph, but this means there are no negative arcs; see the counter example above
            if (pred.isEmpty())
                return;

            List<Edge> chain = improvableChain(graph, pred);
            for (Edge a : chain) {
                if (graph.edges().anyMatch(e -> e == a)) {
                    // a.setAttribute("Delta", (int) a.getAttribute("capacity") - (double) a.getAttribute("flow"));
                    a.setAttribute("Delta", (double) a.getAttribute("capacity", Integer.class));
                } else {
                    a.setAttribute("Delta", (double) a.getAttribute("flow"));
                }
            }   
            double delta = (double) chain.stream().min((e1, e2) -> (int) ((double) e1.getAttribute("Delta") - (double) e2.getAttribute("Delta"))).orElseThrow().getAttribute("Delta");
            for (Edge a : chain) {
                if (graph.edges().anyMatch(e -> e == a)) {
                    setFlow(a, (double) a.getAttribute("flow") + delta);
                    a.setAttribute("capacity", (int) a.getAttribute("capacity") - (int) delta);
                } else {
                    setFlow(a, (double) a.getAttribute("flow") - delta);
                    a.setAttribute("capacity", (int) a.getAttribute("capacity") + (int) delta);
                }
            }
        } while (true);
    }

    static void edmondsKarpBFH2(Graph graph) {
        /* real: delta;
        table: pred; // declaration of the table of vertices
        table: flow; // declaration of the table of the flow on the arcs
        table: Delta; // declaration of the table of defaults
        file: C; // declaration of the list of arcs
        begin
            // initialisation of the flow on the arcs and defaults
            foreach a in E do
                flow[a] := 0;
                Delta[a] := Inf;
            end
            // call on breadth-first traversal to find an improvable path
            repeat
                pred := Breadth_traversal(R);
                if pred == -1 then return flow;
                // Compute an improvable chain C
                C := Improvable_chain(R);
                // Compute the minimal default delta = Delta_C on C
                foreach a in C do
                    if a is a positive arc of C then Delta[a] := c(a) - flow[a];
                    else Delta[a] := flow[a];
                end
                delta := min{Delta[a] | a in C};
                foreach a in C do
                    if a is a positive arc of C then
                        flow[a] := flow[a] + delta;
                        c(a) := c(a) - delta;
                    else
                        c(a) := c(a) + delta;
                    end
                end
            endrepeat
        end */

        for (Edge a : graph.edges().toList()) {
            setFlow(a, 0);
            a.setAttribute("Delta", Double.POSITIVE_INFINITY);
        }

        Map<Node, Node> pred;
        do {
            Graph uGraph = graph;
            pred = breadthTraversalBFH(uGraph); // uses the underlying graph (see method definition)
            if (pred.isEmpty())
                break;

            List<Arc> chain = impChain(uGraph, pred);
            for (Arc a : chain) {
                Edge e = getEdge(graph, a);
                if (isPositive(graph, a)) {
                    // e.setAttribute("Delta", (int) e.getAttribute("capacity") - (double) e.getAttribute("flow"));
                    e.setAttribute("Delta", (double) e.getAttribute("capacity", Integer.class));
                } else {
                    e.setAttribute("Delta", (double) e.getAttribute("flow"));
                }
            }

            final Graph g = graph;
            double delta = chain.stream().map(a -> (double) getEdge(g, a).getAttribute("Delta"))
                .min(Double::compare).orElseThrow();
            
            for (Arc a : chain) {
                Edge e = getEdge(graph, a);
                if (isPositive(graph, a)) {
                    setFlow(e, (double) e.getAttribute("flow") + delta);
                    e.setAttribute("capacity", (int) e.getAttribute("capacity") - (int) delta);
                } else {
                    setFlow(e, (double) e.getAttribute("flow") - delta);
                    e.setAttribute("capacity", (int) e.getAttribute("capacity") + (int) delta);
                }
            }
        } while (true);
    }

    private static void awaitContinue() {
        synchronized (STEP_LOCK) {
            while (!nextStep) {
                try {
                    STEP_LOCK.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            nextStep = false;
        }
    }

    private static void highlightPath(Graph graph, List<Arc> chain) {
        clearHighlight(graph);

        for (Arc a : chain) {
            Edge e = getEdge(graph, a);
            e.setAttribute("ui.class", "active");
            viewer.replayGraph(graph);
            try { Thread.sleep(200); } catch (InterruptedException e1) { e1.printStackTrace(); Thread.currentThread().interrupt(); }
        }
    }

    private static void clearHighlight(Graph graph) {
        for (Edge e : graph.edges().toList())
            e.removeAttribute("ui.class");
    }

    private static Set<Node> reachableVertices(Graph residual, Node source) {
        Set<Node> visited = new HashSet<>();
        Deque<Node> queue = new ArrayDeque<>();

        // Initialize with the source node
        visited.add(source);
        queue.add(source);

        while (!queue.isEmpty()) {
            Node current = queue.pollFirst();

            // Find all outgoing edges from the current node with capacity > 0
            List<Edge> validEdges = residual.edges()
                .filter(e -> e.getSourceNode() == current)
                .filter(e -> ((int) e.getAttribute("capacity")) > 0)
                .filter(e -> !visited.contains(e.getTargetNode()))
                .toList();

            for (Edge e : validEdges) {
                Node neighbor = e.getTargetNode();
                visited.add(neighbor);
                queue.add(neighbor);
            }
        }

        return visited;
    }

    private static void showResult(Graph graph) {
        clearHighlight(graph);

        for (Edge e : graph.edges().toList()) {
            if ((double) e.getAttribute("flow") > 0) {
                e.setAttribute("ui.class", "actFlow");
                try { Thread.sleep(200); } catch (InterruptedException e1) { e1.printStackTrace(); Thread.currentThread().interrupt(); }
            }
        }

        Graph residual = getResidualGraph(graph);
        Set<Node> X = reachableVertices(residual, residual.getNode("A"));
        Set<String> reachableIds = X.stream()
            .map(Node::getId)
            .collect(Collectors.toSet());

        for (Node v : graph) {
            if (reachableIds.contains(v.getId())) {
                v.setAttribute("ui.class", "sourceCut");
            } else {
                v.setAttribute("ui.class", "sinkCut");
            }
        }
    }

    static void edmondsKarp(Graph graph) {
        awaitContinue();

        for (Edge a : graph.edges().toList()) {
            setFlow(a, 0);
            a.setAttribute("Delta", Double.POSITIVE_INFINITY);
        }

        awaitContinue();

        Map<Node, Node> pred;
        do {
            Graph rGraph = getResidualGraph(graph);
            pred = breadthTraversal(rGraph);
            if (pred.isEmpty())
                break;

            List<Arc> chain = impChain(rGraph, pred);
            highlightPath(graph, chain);
            awaitContinue();

            for (Arc a : chain) {
                Edge e = getEdge(graph, a);
                if (isPositive(graph, a)) {
                    e.setAttribute("Delta", (int) e.getAttribute("capacity") - (double) e.getAttribute("flow"));
                } else {
                    e.setAttribute("Delta", (double) e.getAttribute("flow"));
                }
            }   
            
            final Graph g = graph;
            double delta = chain.stream().map(a -> (double) getEdge(g, a).getAttribute("Delta"))
                .min(Double::compare).orElseThrow();
            
            for (Arc a : chain) {
                Edge e = getEdge(graph, a);
                if (isPositive(graph, a)) {
                    setFlow(e, (double) e.getAttribute("flow") + delta);
                } else {
                    setFlow(e, (double) e.getAttribute("flow") - delta);
                }
            }

            awaitContinue();
            clearHighlight(rGraph);
        } while (true);

        showResult(graph);
        awaitContinue();
        viewer.close();
        System.exit(0);
    }

    static Graph copyGraph(Graph original) {
        Graph copy = new SingleGraph(original.getId() + "_copy");

        // copy graph attributes
        for (String attr : original.attributeKeys().toList()) {
            copy.setAttribute(attr, original.getAttribute(attr));
        }

        // copy nodes
        for (Node n : original.nodes().toList()) {

            Node newNode = copy.addNode(n.getId());

            for (String attr : n.attributeKeys().toList()) {
                newNode.setAttribute(attr, n.getAttribute(attr));
            }
        }

        // copy edges
        for (Edge e : original.edges().toList()) {

            Edge newEdge = copy.addEdge(
                e.getId(),
                e.getSourceNode().getId(),
                e.getTargetNode().getId(),
                e.isDirected()
            );

            for (String attr : e.attributeKeys().toList()) {
                newEdge.setAttribute(attr, e.getAttribute(attr));
            }
        }

        copy.setAttribute("source", copy.getNode("A"));
        copy.setAttribute("sink", copy.getNode(String.valueOf((char) (copy.getNode("A").getId().charAt(0) + copy.getNodeCount() - 1))));

        return copy;
    }

    private static Graph getResidualGraph(Graph graph) {
        Graph rGraph = copyGraph(graph);
        for (Edge e : rGraph.edges().toList()) {
            rGraph.removeEdge(e);
        }
        
        for (Edge e : graph.edges().toList()) {
            if (e.getAttribute("flow", Double.class) > 0) {
                Edge bEdge = rGraph.addEdge(e.getTargetNode().getId() + e.getSourceNode().getId(), e.getTargetNode().getId(), e.getSourceNode().getId(), true);
                bEdge.setAttribute("capacity", e.getAttribute("flow", Double.class).intValue());
            }
            if (e.getAttribute("flow", Double.class) < e.getAttribute("capacity", Integer.class)) {
                Edge fEdge = rGraph.addEdge(e.getSourceNode().getId() + e.getTargetNode().getId(), e.getSourceNode().getId(), e.getTargetNode().getId(), true);
                fEdge.setAttribute("capacity", (int) (e.getAttribute("capacity", Integer.class) - e.getAttribute("flow", Double.class)));
            }
        }

        return rGraph;
    }

    static boolean isPositive(Graph graph, Arc a) {
        return graph.getEdge(a.from.getId() + a.to.getId()) != null;
    }

    static Edge getEdge(Graph graph, Arc a) {
        return graph.getEdge(a.from.getId() + a.to.getId()) != null
                ? graph.getEdge(a.from.getId() + a.to.getId())
                : graph.getEdge(a.to.getId() + a.from.getId());
    }

    static Map<Node, Node> breadthTraversal(Graph graph) {
        Map<Node, Node> pred = new HashMap<>();
        Deque<Node> q = new ArrayDeque<>();

        for (Node x : graph.nodes().toList()) {
            pred.put(x, null);
        }

        Node s = (Node) graph.getAttribute("source");
        Node p = (Node) graph.getAttribute("sink");
        q.add(s);

        while(!q.isEmpty()) {
            Node x = q.pollFirst();
            List<Edge> edges = graph.edges()
                .filter(e -> e.getSourceNode() == x)
                .filter(e -> ((int) e.getAttribute("capacity")) > 0)
                .filter(e -> pred.get(e.getTargetNode()) == null)
                .toList();
            
            for (Edge e : edges) {
                Node y = e.getTargetNode();
                pred.put(y, x);

                if (y == p)
                    return pred;

                q.add(y);
            }
        }

        return Collections.emptyMap();
    }

    static Map<Node, Node> breadthTraversalBFH(Graph graph) {
        Map<Node, Node> pred = new HashMap<>();
        Deque<Node> q = new ArrayDeque<>();

        for (Node x : graph.nodes().toList()) {
            pred.put(x, null);
        }

        Node s = (Node) graph.getAttribute("source");
        Node p = (Node) graph.getAttribute("sink");
        q.add(s);

        while(!q.isEmpty()) {
            Node x = q.pollFirst();
            List<Edge> edges = graph.edges()
                .filter(e -> e.getNode0() == x || e.getNode1() == x) // treat the graph as undirected
                .filter(e -> e.getAttribute("capacity", Integer.class) > 0) // does not consider negative arcs, thus breaks
                .filter(e -> pred.get(e.getOpposite(x)) == null)
                .toList();
            
            for (Edge e : edges) {
                Node y = e.getOpposite(x);
                pred.put(y, x);

                if (y == p)
                    return pred;

                q.add(y);
            }
        }

        return Collections.emptyMap();
    }

    static List<Edge> improvableChain(Graph graph, Map<Node, Node> pred) {
        Node x = (Node) graph.getAttribute("sink");
        Node s = (Node) graph.getAttribute("source");

        List<Edge> chain = new ArrayList<>();

        while (x != s) {
            Node finalX = x;
            chain.add(graph.edges().filter(e -> e.getSourceNode() == pred.get(finalX) && e.getTargetNode() == finalX).findFirst().orElseThrow());
            x = pred.get(x);
        }

        return chain;
    }

    record Arc(Node from, Node to) {}

    static List<Arc> impChain(Graph graph, Map<Node, Node> pred) {
        Node x = (Node) graph.getAttribute("sink");
        Node s = (Node) graph.getAttribute("source");

        List<Arc> chain = new ArrayList<>();

        while (x != s) {
            chain.add(0, new Arc(pred.get(x), x)); // always insert front, so the chain is not reversed (although it shouldn't really matter)
            x = pred.get(x);
        }

        return chain;
    }

    static final String STYLE = """
        node {
            size: 25px;
            fill-color: lightgray;
            text-size: 18;
            text-alignment: above;
        }

        edge {
            text-size: 16px;
        }

        node.current {
            fill-color: red;
        }

        node.visited {
            fill-color: green;
        }

        edge {
            fill-color: gray;
            arrow-size: 10px, 6px;
        }

        edge.active {
            fill-color: orange;
            size: 3px;
        }

        edge.actFlow {
            fill-color: lightblue;
            size: 3px;
        }

        edge.cut {
            fill-color: red;
            size: 5px;
        }

        node.sourceCut {
            fill-color: gold;
        }

        node.sinkCut {
            fill-color: lightgreen;
        }
        """;
}

