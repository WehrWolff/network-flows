package flows;

import com.formdev.flatlaf.FlatLightLaf;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.GraphParseException;
import org.graphstream.stream.SinkAdapter;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.util.InteractiveElement;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;

public class NewJFrame extends JFrame {
    
    private static final Logger logger = Logger.getLogger(NewJFrame.class.getName());

    private JPopupMenu popup = null;

    private MouseEvent pendingClick;
    private final Timer clickTimer = new Timer((Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval"), e -> {
        if (SwingUtilities.isLeftMouseButton(pendingClick)) {
            onLeftSingleClick(pendingClick);
        } else if (SwingUtilities.isRightMouseButton(pendingClick)) {
            onRightSingleClick(pendingClick);
        }
    });

    private enum PremadeGraphs {
        TRAIN_GRAPH("Train graph", "train_graph.dgs"),
        PATHOLOGICAL_FF("Pathological Ford-Fulkerson", "pathological_ff.dgs");

        private final String displayName;
        private final String file;
        PremadeGraphs(String displayName, String file) {
            this.displayName = displayName;
            this.file = file;
        }

        public String getFile() {
            return PremadeGraphs.class.getResource("/" + file).getPath();
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private transient Graph graph = new MultiGraph("graph");
    private transient Viewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
    private ViewPanel view = (ViewPanel) viewer.addDefaultView(false);

    protected class LabelUpdater extends SinkAdapter {
        @Override
        public void nodeAdded(String sourceId, long timeId, String nodeId) {
            Node n = graph.getNode(nodeId);
            n.setAttribute("ui.label", n.getId());
        }

        @Override
        public void edgeAttributeAdded(String sourceId, long timeId, String edgeId, String attribute, Object newValue) {
            updateEdgeLabel(edgeId, attribute);
        }

        @Override
        public void edgeAttributeChanged(String sourceId, long timeId, String edgeId, String attribute, Object oldValue, Object newValue) {
            updateEdgeLabel(edgeId, attribute);
        }

        private void updateEdgeLabel(String edgeId, String attribute) {
            if (attribute.equals("capacity") || attribute.equals("flow")) {
                Edge edge = graph.getEdge(edgeId);
                
                Double flow = edge.getAttribute("flow", Double.class);
                Integer cap = edge.getAttribute("capacity", Integer.class);

                if (flow == null) {
                    edge.setAttribute("ui.label", String.valueOf(cap));
                } else {
                    edge.setAttribute("ui.label", flow + "/" + cap);
                }
            }
        }
    }

    /**
     * Creates new form NewJFrame
     */
    public NewJFrame() {
        clickTimer.setRepeats(false);

        graph.setAttribute("ui.quality");
        graph.setAttribute("ui.antialias");
        graph.addSink(new LabelUpdater());

        try {
            graph.setAttribute("ui.stylesheet", Files.readString(Path.of(NewJFrame.class.getResource("/style.css").toURI())));
        } catch (IOException | URISyntaxException ex) {
            logger.log(Level.SEVERE, ex.toString(), ex);
        }

        initComponents();
    }

    private void initComponents() {
        setTitle("Network Flows");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new CardLayout(5, 5));
        
        jTabbedPane1 = new JTabbedPane();
        jTabbedPane1.setPreferredSize(new Dimension(1280, 675));
        jTabbedPane1.addChangeListener(this::jTabbedPane1TabChanged);

        graphTabPanel = new GraphTabPanel();
        graphTabPanel.setHtml("""
            <html>
                <br>
                <center><b>Decide on which graph the algorithm should be performed!</b></center>
                <br>
                You can choose from premade graphs:
                <br><br>
                Alternatively you can select a graph from your local files. For more information on the file format refer to the README on
                <a href="https://github.com/WehrWolff/network-flows?tab=readme-ov-file" target="_blank">GitHub</a>.
                <br><br><br><br>
                You can also change the graph right here:
                <ul>
                    <li>To <u>add a node</u> (vertex) double click in the graph area.</li><br>
                    <li>To <u>create an edge</u> (arc) left click the source then the target node.</li><br>
                    <li>To <u>change the capacity</u> double click on the corresponding edge label.</li><br>
                    <li>To <u>delete</u> nodes and edges right click them. For edges right click the edge label.</li>
                </ul>
                Save your graph to a local file:
                <br><br><br><br>
                So what now?
            </html>
        """);
        graphTabPanel.getEditorPane().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                jEditorPane1MouseClicked();
            }
        });

        view.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                jGraphPanelActionPerformed(evt);
            }
        });

        jComboBox1 = new JComboBox<>();
        jComboBox1.setModel(new DefaultComboBoxModel<>(PremadeGraphs.values()));
        graphTabPanel.addOverlay(jComboBox1, 270, 95, 100, 24);
        jComboBox1.addActionListener(e -> {
            PremadeGraphs pgraph = (PremadeGraphs) jComboBox1.getSelectedItem();
            try {
                emptyGraph(graph);
                graph.read(pgraph.getFile());
            } catch (IOException | GraphParseException ex) {
                ex.printStackTrace();
            }
        });

        JButton jButton1 = new JButton();
        jButton1.setText("Browse filesystem");
        jButton1.setFocusPainted(false);
        jButton1.addActionListener(this::jButton1ActionPerformed);
        graphTabPanel.addOverlay(jButton1, 120, 200, 170, 24);
        
        JButton jButton2 = new JButton();
        jButton2.setText("Save to file");
        jButton2.setFocusPainted(false);
        jButton2.addActionListener(this::jButton2ActionPerformed);
        graphTabPanel.addOverlay(jButton2, 120, 505, 170, 24);
        
        JButton jButton3 = new JButton();
        jButton3.setBackground(new Color(222, 230, 237));
        jButton3.setText("<html><center>Next Step: Turn graph into network</center></html>");
        jButton3.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(38, 117, 191), 3), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        jButton3.addActionListener(this::jButton3ActionPerformed);
        graphTabPanel.addOverlay(jButton3, 90, 580, 230, 34);

        jTabbedPane1.addTab("Create a graph", graphTabPanel);



        graphTabPanel2 = new GraphTabPanel();
        graphTabPanel2.setHtml("""
            <html>
                <br>
                <center><b>Not every directed graph is a network, maybe some changes are due!</b></center>
            </html>
            """);

        cardsPanel = new Accordion();

        AccordionCard source = new AccordionCard("Exactly one source", e -> { createSuperSource(); updateCardsPanel(0); });
        AccordionCard sink = new AccordionCard("Exactly one sink", e -> { createSuperSink(); updateCardsPanel(1); });
        AccordionCard loops = new AccordionCard("No loops", e -> { removeLoops(); updateCardsPanel(2); });
        AccordionCard multiarcs = new AccordionCard("No multiarcs", e -> { mergeMultiarcs(); updateCardsPanel(3); });
        AccordionCard capacity = new AccordionCard("Every arc has a capacity", e -> { addSufficientCap(); updateCardsPanel(4); });

        source.setTexts("Every network requires exactly one source (that is a node with only outgoing arcs).", "Create super source");
        sink.setTexts("Every network requires exactly one sink (that is a node with only incoming arcs).", "Create super sink");
        loops.setTexts("A network may not have loops (arcs with the same start and target node).", "Remove loops");
        multiarcs.setTexts("A network may not have multiarcs (multiple arcs with the same source and target node).", "Merge multiarcs");
        capacity.setTexts("Every arc of a network needs a capacity. The added capacities will be sufficiently large, so that they never are a bottleneck.", "Add sufficient capacities");

        cardsPanel.addCard(source);
        cardsPanel.addCard(sink);
        cardsPanel.addCard(loops);
        cardsPanel.addCard(multiarcs);
        cardsPanel.addCard(capacity);

        graphTabPanel2.addOverlay(cardsPanel, 15, 100, 360, cardsPanel.getPreferredSize().height);

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setFocusable(false);

        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        editorPane.setContentType("text/html"); // NOI18N
        editorPane.setAutoscrolls(false);
        editorPane.setFocusCycleRoot(false);
        editorPane.setFocusable(false);
        scrollPane.setViewportView(editorPane);

        editorPane.setText("Hi!");
        cardsPanel.add(scrollPane, JLayeredPane.MODAL_LAYER);

        jTabbedPane1.addTab("Turn into network", graphTabPanel2);


        JPanel jPanel3 = new JPanel();
        jPanel3.setBorder(BorderFactory.createEtchedBorder());

        GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jTabbedPane1.addTab("Apply an algorithm", jPanel3);

        getContentPane().add(jTabbedPane1, "card2");

        pack();
        setLocationRelativeTo(null);
    }

    private void jTabbedPane1TabChanged(ChangeEvent evt) {
        int tab = jTabbedPane1.getSelectedIndex();
        
        if (tab == 0) {
            graphTabPanel.setGraphView(viewer, view);
            jEditorPane1MouseClicked();
        } else if (tab == 1) {
            graphTabPanel2.setGraphView(viewer, view);
            updateCardsPanel(4);
        }
    }

    private void updateCardsPanel(int index) {
        cardsPanel.satisfyCard(0, hasSingleSource());
        cardsPanel.satisfyCard(1, hasSingleSink());
        cardsPanel.satisfyCard(2, hasNoLoops());
        cardsPanel.satisfyCard(3, hasNoMultiarcs());
        cardsPanel.satisfyCard(4, hasCapacities());
        activateNextCardFrom(index);
    }

    private void activateNextCardFrom(int index) {
        boolean activated = false;
        for (int i = index + 5; i > index; i--) {
            if (!cardsPanel.isSatisfied(i % 5)) {
                cardsPanel.activateCard(i % 5);
                activated |= true;
            }
        }

        if(!activated)
            cardsPanel.deactivateAll();
    }

    public static void emptyGraph(Graph graph) {
        while (graph.getEdgeCount() > 0)
            graph.removeEdge(0);

        while (graph.getNodeCount() > 0)
            graph.removeNode(0);
    }

    public static String genNodeID(Graph graph) {
        String out = "v";
        int i = 0;

        while (graph.getNode(out + i) != null) { i++; }
        return out + i;
    }

    public static String genEdgeID(Graph graph) {
        String out = "e";
        int i = 0;

        while (graph.getEdge(out + i) != null) { i++; }
        return out + i;
    }

    private void jButton1ActionPerformed(ActionEvent evt) {                                         
        JFileChooser chooser = new JFileChooser();
        int status = chooser.showOpenDialog(this);
        File file = chooser.getSelectedFile();
        
        if (status == JFileChooser.APPROVE_OPTION && file != null) {
            
            try {
                emptyGraph(graph);
                graph.read(file.getAbsolutePath()); // TODO: make sure there are no (future) id clashes
            } catch (IOException | GraphParseException ex) {
                ex.printStackTrace();
            }

            graphTabPanel.setGraphView(viewer, view);
        } else {
            logger.info("No file was chosen.");
        } 
    }                                        

    private void jButton2ActionPerformed(ActionEvent evt) {                                         
        JFileChooser chooser = new JFileChooser();
        
        File file = new File(chooser.getCurrentDirectory(), "untitled1.dgs");
        int i = 1;
        while (file.exists()) {
            i++;
            file = new File("untitled" + i + ".dgs");
        }
        
        chooser.setSelectedFile(file);
        int status = chooser.showSaveDialog(this);
        file = chooser.getSelectedFile();
        
        if (status == JFileChooser.APPROVE_OPTION && file != null) {
            try {
                graph.write(file.getAbsolutePath());
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        } else {
            logger.info("No file was chosen.");
        }
    }                                        

    private void jButton3ActionPerformed(ActionEvent evt) {
        jTabbedPane1.setSelectedIndex(1);
    }

    private void jEditorPane1MouseClicked() {
        // Put focus on to the graph panel (so that combobox focus is registered correctly at all times)
        graphTabPanel.requestFocus();
        // Defocus the field (but allow focus highlight for the next time)
        jComboBox1.setFocusable(false);
        jComboBox1.setFocusable(true);
    }

    private void jGraphPanelActionPerformed(MouseEvent evt) {
        if (jTabbedPane1.getSelectedIndex() != 0)
            return;

        if (popup != null)
            popup.setVisible(false);

        if (evt.getClickCount() == 1) {
            pendingClick = evt;
            clickTimer.restart();
        }

        if (evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt)) {
            clickTimer.stop();
            Collection<GraphicElement> elements = view.allGraphicElementsIn(EnumSet.of(InteractiveElement.EDGE), evt.getX() - 6f, evt.getY() - 6f, evt.getX() + 6f, evt.getY() + 6f);

            if (elements.isEmpty()) {
                Point3 p = view.getCamera().transformPxToGu(evt.getPoint().x, evt.getPoint().y);
                Node n = graph.addNode(genNodeID(graph));
                n.setAttribute("xyz", p.x, p.y, p.z);
            } else {
                Edge e = graph.getEdge(elements.stream().findFirst().orElseThrow().getId());
                
                JSpinner spinner = new JSpinner(new SpinnerNumberModel(e.getAttribute("capacity", Integer.class), 0, null, 1));
                spinner.addChangeListener(cl -> e.setAttribute("capacity", (Integer) spinner.getValue()));

                popup = new JPopupMenu();
                popup.setLayout(new BorderLayout());
                popup.add(spinner);

                popup.show(graphTabPanel.getGraphPanel(), evt.getX(), evt.getY());
            }
        }
    }

    private transient Node connectionStart = null;
    private void onLeftSingleClick(MouseEvent evt) {
        GraphicElement element = view.findGraphicElementAt(EnumSet.of(InteractiveElement.NODE), evt.getX(), evt.getY());

        if (element != null) {
            Node n = graph.getNode(element.getId());

            if (connectionStart == null) {
                connectionStart = n;
            } else {
                Edge e = graph.addEdge(genEdgeID(graph), connectionStart, n, true);
                e.setAttribute("capacity", 0);
                connectionStart = null;
            }
        }
    }

    private void onRightSingleClick(MouseEvent evt) {
        GraphicElement element = view.findGraphicElementAt(EnumSet.of(InteractiveElement.NODE), evt.getX(), evt.getY());
        Collection<GraphicElement> elements = view.allGraphicElementsIn(EnumSet.of(InteractiveElement.EDGE), evt.getX() - 6f, evt.getY() - 6f, evt.getX() + 6f, evt.getY() + 6f);

        if (element != null) {
            Node n = graph.getNode(element.getId());
            graph.removeNode(n);
        } else if (!elements.isEmpty()) {
            Edge e = graph.getEdge(elements.stream().findFirst().orElseThrow().getId());
            graph.removeEdge(e);
        }
    }

    private boolean hasSingleSource() {
        List<Node> sources = graph.nodes().filter(v -> v.getOutDegree() > 0 && v.getInDegree() == 0).toList();
        return sources.size() == 1;
    }

    private void createSuperSource() {
        List<Node> sources = graph.nodes().filter(v -> v.getOutDegree() > 0 && v.getInDegree() == 0).toList();
        
        if (sources.size() == 1) {
            graph.setAttribute("source", sources.getFirst().getId());
            return;
        }

        if (sources.isEmpty()) {
            if (graph.nodes().findAny().isEmpty()) {
                graph.addNode("s");
                return;
            }

            Node n = graph.nodes().max((v1, v2) -> v1.getOutDegree() - v2.getOutDegree()).orElseThrow();
            Node source = graph.addNode("s");
            Edge e = graph.addEdge("s" + n.getId(), source, n, true);

            if (n.getOutDegree() != 0 && n.leavingEdges().allMatch(a -> a.hasAttribute("capacity"))) {
                e.setAttribute("capacity", n.leavingEdges().mapToInt(a -> a.getAttribute("capacity", Integer.class)).sum()); // this assumes they have a capacity
            }
            
            return;
        }

        Node source = graph.addNode("s");
        for (Node si : sources) {
            Edge e = graph.addEdge("s" + si.getId(), source, si, true);

            if (si.getOutDegree() != 0 && si.leavingEdges().allMatch(a -> a.hasAttribute("capacity"))) {
                e.setAttribute("capacity", si.leavingEdges().mapToInt(a -> a.getAttribute("capacity", Integer.class)).sum()); // this assumes they have a capacity
            }
        }
    }

    private boolean hasSingleSink() {
        List<Node> sinks = graph.nodes().filter(v -> v.getInDegree() > 0 && v.getOutDegree() == 0).toList();
        return sinks.size() == 1;
    }

    private void createSuperSink() {
        List<Node> sinks = graph.nodes().filter(v -> v.getInDegree() > 0 && v.getOutDegree() == 0).toList();
        
        if (sinks.size() == 1) {
            graph.setAttribute("sink", sinks.getFirst().getId());
            return;
        }

        if (sinks.isEmpty()) {
            if (graph.nodes().findAny().isEmpty()) {
                graph.addNode("t");
                return;
            }

            Node n = graph.nodes().max((v1, v2) -> v1.getInDegree() - v2.getInDegree()).orElseThrow();
            Node sink = graph.addNode("t");
            Edge e = graph.addEdge(n.getId() + "t", n, sink, true);

            if (n.getInDegree() != 0 && n.enteringEdges().allMatch(a -> a.hasAttribute("capacity"))) {
                e.setAttribute("capacity", n.enteringEdges().mapToInt(a -> a.getAttribute("capacity", Integer.class)).sum());
            }

            return;
        }

        Node sink = graph.addNode("t");
        for (Node ti : sinks) {
            Edge e = graph.addEdge(ti.getId() + "t", ti, sink, true);

            if (ti.getInDegree() != 0 && ti.enteringEdges().allMatch(a -> a.hasAttribute("capacity"))) {
                e.setAttribute("capacity", ti.enteringEdges().mapToInt(a -> a.getAttribute("capacity", Integer.class)).sum());
            }
        }
    }

    private boolean hasNoLoops() {
        return graph.edges().noneMatch(Edge::isLoop);
    }

    private void removeLoops() {
        graph.edges().filter(Edge::isLoop).forEach(
            e -> graph.removeEdge(e)
        );
    }

    private boolean hasNoMultiarcs() {
        Map<String, Edge> kept = new HashMap<>();

        for (Edge e : graph.edges().toList()) {
            String key = e.getSourceNode().getId() + "->" + e.getTargetNode().getId();
            Edge existing = kept.get(key);

            if (existing == null) {
                kept.put(key, e);
            } else {
                return false;
            }
        }

        return true;
    }

    private void mergeMultiarcs() {
        Map<String, Edge> kept = new HashMap<>();

        for (Edge e : graph.edges().toList()) {
            String key = e.getSourceNode().getId() + "->" + e.getTargetNode().getId();

            Edge existing = kept.get(key);
            if (existing == null) {
                kept.put(key, e);
            } else {
                int cap1 = existing.getAttribute("capacity", Integer.class);
                int cap2 = e.getAttribute("capacity", Integer.class);

                existing.setAttribute("capacity", cap1 + cap2);

                graph.removeEdge(e);
            }
        }
    }

    private boolean hasCapacities() {
        return graph.edges().allMatch(e -> e.hasAttribute("capacity"));
    }

    private void addSufficientCap() {
        propagateCapacities(graph);

        // TODO: make sure there is exactly one source / other steps are run before
        graph.edges().filter(e -> !e.hasAttribute("capacity") && e.getSourceNode().getId().equals(graph.getAttribute("source", String.class)))
            .forEach(e -> e.setAttribute("capacity", 2));

        propagateCapacities(graph);

        graph.edges().filter(e -> !e.hasAttribute("capacity"))
            .forEach(e -> e.setAttribute("capacity", 1));
    }

    private static void propagateCapacities(Graph graph) {
        Deque<Edge> queue = new ArrayDeque<>();
        queue.addAll(graph.edges().filter(e -> !e.hasAttribute("capacity")).filter(e -> e.getTargetNode().getOutDegree() != 0 && e.getTargetNode().leavingEdges().allMatch(le -> le.hasAttribute("capacity"))).toList());
        queue.addAll(graph.edges().filter(e -> !e.hasAttribute("capacity")).filter(e -> e.getSourceNode().getInDegree() != 0 && e.getSourceNode().enteringEdges().allMatch(le -> le.hasAttribute("capacity"))).toList());

        while (!queue.isEmpty()) {
            Edge e = queue.poll();
            if (e.getSourceNode().getInDegree() != 0 && e.getSourceNode().enteringEdges().allMatch(a -> a.hasAttribute("capacity"))) {
                e.setAttribute("capacity", e.getSourceNode().enteringEdges().mapToInt(a -> a.getAttribute("capacity", Integer.class)).sum());
            } else {
                e.setAttribute("capacity", e.getTargetNode().leavingEdges().mapToInt(a -> a.getAttribute("capacity", Integer.class)).sum());
            }

            queue.clear();
            queue.addAll(graph.edges().filter(a -> !a.hasAttribute("capacity")).filter(a -> a.getTargetNode().getOutDegree() != 0 && a.getTargetNode().leavingEdges().allMatch(le -> le.hasAttribute("capacity"))).toList());
            queue.addAll(graph.edges().filter(a -> !a.hasAttribute("capacity")).filter(a -> a.getSourceNode().getInDegree() != 0 && a.getSourceNode().enteringEdges().allMatch(ee -> ee.hasAttribute("capacity"))).toList());
        }
    }

    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "swing");
        
        FlatLightLaf.setup();
        
        /* Create and display the form */
        EventQueue.invokeLater(() -> new NewJFrame().setVisible(true));
    }

    // Variables declaration - do not modify
    private GraphTabPanel graphTabPanel;
    private GraphTabPanel graphTabPanel2;
    private JComboBox<PremadeGraphs> jComboBox1;
    private JTabbedPane jTabbedPane1;
    private Accordion cardsPanel;
    // End of variables declaration                   
}
