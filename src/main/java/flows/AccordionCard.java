package flows;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class AccordionCard extends JPanel {

    private final JPanel headerPanel;
    private final JPanel contentPanel;

    private final JLabel titleLabel;
    private final JLabel arrowLabel;

    private final JLabel descriptionLabel;
    private final JButton actionButton;

    private boolean expanded;
    private boolean satisfied;

    public AccordionCard(String title, String description, String action, ActionListener listener) {
        this(title, listener);
        setTexts(description, action);
    }

    public AccordionCard(String title, ActionListener listener) {
        setLayout(new BorderLayout());
        
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));

        /* ---------- header ---------- */

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));

        arrowLabel = new JLabel("▶");

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(arrowLabel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        /* ---------- content ---------- */

        descriptionLabel = new JLabel();
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(12f));

        actionButton = new JButton();
        actionButton.setFocusPainted(false);
        actionButton.addActionListener(listener);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setOpaque(false);
        buttonPanel.add(actionButton);

        contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(Box.createVerticalStrut(2));
        contentPanel.add(descriptionLabel);
        contentPanel.add(Box.createVerticalStrut(3));
        contentPanel.add(buttonPanel);

        add(contentPanel, BorderLayout.CENTER);

        setExpanded(false);
        setSatisfied(true);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;

        arrowLabel.setText(expanded ? "▼" : "▶");
        contentPanel.setVisible(expanded);

        revalidate();
        repaint();
    }

    public void setTexts(String description, String action) {
        descriptionLabel.setText(
                    "<html><body style='width:250px'>" +
                    description +
                    "</body></html>");

        actionButton.setText(action);
    }

    public void setSatisfied(boolean ok) {
        this.satisfied = ok;
        if (ok) {
            titleLabel.setText("✔ " + stripPrefix(titleLabel.getText()));
            titleLabel.setForeground(new Color(0, 130, 0));

            actionButton.setVisible(false);
        } else {
            titleLabel.setText("✘ " + stripPrefix(titleLabel.getText()));
            titleLabel.setForeground(new Color(180, 0, 0));

            actionButton.setVisible(true);
        }

        revalidate();
        repaint();
    }

    public boolean isSatisfied() {
        return satisfied;
    }

    public void setActionListener(ActionListener l) {

        for (ActionListener old : actionButton.getActionListeners())
            actionButton.removeActionListener(old);

        actionButton.addActionListener(l);
    }

    public void fireMouseClicked() {
        for (MouseListener ml : headerPanel.getMouseListeners()) {
            ml.mouseClicked(null);
        }
    }

    public JButton getActionButton() {
        return actionButton;
    }

    public void addHeaderMouseListener(MouseListener listener) {
        headerPanel.addMouseListener(listener);
    }

    private static String stripPrefix(String s) {
        if (s.startsWith("✔ ") || s.startsWith("✘ "))
            return s.substring(2);

        return s;
    }
}