package com.jonbackhaus.visualizer;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

import java.awt.event.ActionEvent;

public class MetacrawlerAction extends MDAction {

    private final Element targetElement;

    public MetacrawlerAction(Element targetElement, String name) {
        super(targetElement.getID(), name, null, null);
        this.targetElement = targetElement;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Application.getInstance().getProject().getBrowser().getContainmentTree().openNode(targetElement, true);
    }
}
