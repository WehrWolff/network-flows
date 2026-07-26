package flows;

import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class Accordion extends JPanel {

    private AccordionCard expandedCard;
    private List<AccordionCard> cards = new ArrayList<>();

    public Accordion() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
    }

    public void addCard(AccordionCard card) {
        if (!cards.isEmpty())
            add(Box.createVerticalStrut(8));

        cards.add(card);
        add(card);

        card.addHeaderMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (expandedCard == card) {
                    card.setExpanded(false);
                    expandedCard = null;
                    updateSize();
                    return;
                }

                if (expandedCard != null)
                    expandedCard.setExpanded(false);

                card.setExpanded(true);
                expandedCard = card;
                updateSize();
            }
        });
    }

    public void activateCard(int index) {
        if (expandedCard != cards.get(index))
            cards.get(index).fireMouseClicked();
    }

    public void deactivateAll() {
        int index = cards.indexOf(expandedCard);
        if (index != -1)
            cards.get(index).fireMouseClicked();
    }

    public void satisfyCard(int index, boolean ok) {
        cards.get(index).setSatisfied(ok);
    }

    public boolean isSatisfied(int index) {
        return cards.get(index).isSatisfied();
    }

    private void updateSize() {
        Rectangle r = getBounds();
        setBounds(r.x, r.y, r.width, getPreferredSize().height);
        revalidate();
        repaint();
    }
}