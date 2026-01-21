package com.jonbackhaus.visualizer;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import javax.jmi.reflect.RefObject;
import org.omg.mof.model.MofAttribute;
import org.omg.mof.model.Reference;
import org.omg.mof.model.ModelElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MetacrawlerService {

    // Global cache for metamodel properties (once discovered, they don't change).
    private static final Map<String, List<ModelElement>> metamodelCache = new ConcurrentHashMap<>();

    /**
     * Populates the Metacrawler menu for the immediate next level only.
     * Structure: Element -> Properties -> Target Elements (Actions)
     */
    public static void populatePropertyMenu(ActionsCategory parentCategory, Element element) {
        if (element == null)
            return;

        // Get a snapshot of the properties to be absolutely safe from concurrent
        // modification
        List<ModelElement> properties = new ArrayList<>(getCachedMetamodelProperties(element));

        for (ModelElement propDef : properties) {
            String propName = "";
            try {
                propName = propDef.getName();
            } catch (Exception e) {
                continue;
            }

            // getTargetElements already returns a fresh ArrayList snapshot
            List<Element> targets = getTargetElements(element, propName);
            if (targets.isEmpty())
                continue;

            // Property Submenu
            ActionsCategory propertyCategory = new ActionsCategory(propDef.refMofId(), propName);
            propertyCategory.setNested(true);

            for (Element target : targets) {
                String targetLabel = RepresentationTextCreator.getRepresentedText(target);

                // Add action to select this target
                propertyCategory.addAction(new MetacrawlerAction(target, targetLabel));
            }

            parentCategory.addAction(propertyCategory);
        }
    }

    private static List<ModelElement> getCachedMetamodelProperties(Element element) {
        if (!(element instanceof RefObject))
            return Collections.emptyList();

        RefObject metaObject = ((RefObject) element).refMetaObject();
        String mofId = metaObject.refMofId();

        return metamodelCache.computeIfAbsent(mofId, id -> {
            List<ModelElement> props = new ArrayList<>();
            if (metaObject instanceof org.omg.mof.model.Class) {
                collectProperties((org.omg.mof.model.Class) metaObject, props, new HashSet<>());
            }

            // Sort once before caching
            props.sort(Comparator.comparing(p -> {
                try {
                    return p.getName();
                } catch (Exception e) {
                    return "";
                }
            }));

            // Return as unmodifiable list
            return Collections.unmodifiableList(props);
        });
    }

    private static void collectProperties(org.omg.mof.model.Class mofClass, List<ModelElement> props,
            Set<org.omg.mof.model.Class> visited) {
        if (mofClass == null || !visited.add(mofClass))
            return;

        try {
            // Take a snapshot of contents to avoid CMOD if metamodel changes
            Object[] contents = mofClass.getContents().toArray();
            for (Object content : contents) {
                if (content instanceof MofAttribute || content instanceof Reference) {
                    props.add((ModelElement) content);
                }
            }

            // Take a snapshot of supertypes
            Object[] supertypes = mofClass.getSupertypes().toArray();
            for (Object supertype : supertypes) {
                if (supertype instanceof org.omg.mof.model.Class) {
                    collectProperties((org.omg.mof.model.Class) supertype, props, visited);
                }
            }
        } catch (Exception e) {
        }
    }

    private static List<Element> getTargetElements(Element element, String propertyName) {
        List<Element> targets = new ArrayList<>();
        if (element instanceof RefObject) {
            try {
                Object value = ((RefObject) element).refGetValue(propertyName);
                if (value instanceof Element) {
                    targets.add((Element) value);
                } else if (value instanceof Collection) {
                    // CRITICAL: Snapshot the collection immediately to avoid
                    // ConcurrentModificationException
                    // MagicDraw's JMI collections are often "live" and can change in background
                    // threads.
                    Object[] items = ((Collection<?>) value).toArray();
                    for (Object item : items) {
                        if (item instanceof Element) {
                            targets.add((Element) item);
                        }
                    }
                }
            } catch (Exception e) {
                // Return empty if property access fails or collection is modified during
                // toArray
            }
        }
        return targets;
    }
}
