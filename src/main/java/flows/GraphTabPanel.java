package flows;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JEditorPane;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.LayoutStyle;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;

import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

public class GraphTabPanel extends JPanel {

    private final JSplitPane splitPane = new JSplitPane();
    private final JPanel graphPanel = new JPanel();
    private final JPanel controlPanel = new JPanel();
    private final JSeparator jSeparator1 = new JSeparator();
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JScrollPane scrollPane = new JScrollPane();
    private final JEditorPane editorPane = new JEditorPane();

    private JPanel graphViewHoldingPane = null;

    public GraphTabPanel() {
        initComponents();
    }

    private void initComponents() {
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (IOException | URISyntaxException ex) { /* fail silently */ }
            }
        });

        setBorder(BorderFactory.createEtchedBorder());
        setPreferredSize(new Dimension(1280, 675));

        splitPane.setDividerLocation(861);
        splitPane.setDividerSize(3);
        splitPane.setResizeWeight(1.0);
        splitPane.setOneTouchExpandable(true);
        splitPane.setPreferredSize(new Dimension(1280, 675));

        graphPanel.setLayout(new BorderLayout());

        splitPane.setLeftComponent(graphPanel);

        controlPanel.setPreferredSize(new Dimension(406, 636));

        jSeparator1.setOrientation(SwingConstants.VERTICAL);

        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setFocusable(false);

        editorPane.setEditable(false);
        editorPane.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        editorPane.setContentType("text/html"); // NOI18N
        editorPane.setAutoscrolls(false);
        editorPane.setFocusCycleRoot(false);
        editorPane.setFocusable(false);
        scrollPane.setViewportView(editorPane);

        layeredPane.add(scrollPane);
        scrollPane.setBounds(6, 6, 379, 624);

        GroupLayout controlPanelLayout = new GroupLayout(controlPanel);
        controlPanel.setLayout(controlPanelLayout);
        controlPanelLayout.setHorizontalGroup(
            controlPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSeparator1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(layeredPane)
                .addContainerGap())
        );
        controlPanelLayout.setVerticalGroup(
            controlPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1)
            .addComponent(layeredPane)
        );

        splitPane.setRightComponent(controlPanel);

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(splitPane, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 1276, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(splitPane, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 636, Short.MAX_VALUE)
        );
    }

    public JPanel getGraphPanel() {
        return graphPanel;
    }

    public JLayeredPane getLayeredPane() {
        return layeredPane;
    }

    public JEditorPane getEditorPane() {
        return editorPane;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public void setHtml(String html) {
        editorPane.setText(html);
    }

    public void addOverlay(Component c, int x, int y, int w, int h) {
        layeredPane.add(c, JLayeredPane.MODAL_LAYER);
        c.setBounds(x, y, w, h);
    }

    public void setGraphView(Viewer viewer, ViewPanel view) {
        if (graphViewHoldingPane != null)
            graphViewHoldingPane.remove(view);

        viewer.enableAutoLayout();
        
        graphPanel.setLayout(new BorderLayout());
        graphPanel.add(view, BorderLayout.CENTER);
        graphViewHoldingPane = graphPanel;
        
        graphPanel.revalidate();
        graphPanel.repaint();
    }
}