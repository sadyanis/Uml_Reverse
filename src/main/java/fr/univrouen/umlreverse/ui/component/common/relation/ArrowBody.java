package fr.univrouen.umlreverse.ui.component.common.relation;

import fr.univrouen.umlreverse.ui.component.clazz.dialog.DialogRemovePoint;
import fr.univrouen.umlreverse.util.Contract;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

/**
 * Implements IArrowBody.
 */
public class ArrowBody implements IArrowBody {
// ATTRIBUTES
	public static final String PROP_CIRCLE = "circleList";
    private static final Color COLOR_DEFAULT = Color.BLACK;
    private static final double RADIUS = 5d;
    private Color colorLines;  
    private final ObjectProperty<Point2D> startPointProperty;
    private final ObjectProperty<Point2D> endPointProperty;
    private final Line firstLine;
    private final List<Circle> circlesList;
    private final Map<Circle, ObjectProperty<Point2D>> circles;
    private final Map<ObjectProperty<Point2D>, ChangeListener<Point2D>> changeListerners;
    private final List<Line> lines;
    private final Map<Line, ObjectProperty<Point2D>> linesToPointsStart;
    private final Map<Line, ObjectProperty<Point2D>> linesToPointsEnd;
    private EventHandler<MouseEvent> pointDnd_Event;
    private double dash = -1;
    // ajouter par @yanis
    PropertyChangeSupport pcs ;
    
// CONSTRUCTORS
    /**
     * 
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param tailProperty
     * @param headProperty 
     * @pre
     *      tailProperty != null && headProperty != null
     */
    public ArrowBody(double startX, double startY, double endX, double endY, ObjectProperty<Point2D> tailProperty, ObjectProperty<Point2D> headProperty) {
        Contract.check(tailProperty != null, "tailProperty must not be null.");
        Contract.check(headProperty != null, "headProperty must not be null.");
        //Ajouter par @yanis
        pcs =new PropertyChangeSupport(this);
        colorLines = COLOR_DEFAULT;
        startPointProperty = new SimpleObjectProperty<>();
        endPointProperty = new SimpleObjectProperty<>();
        firstLine = new Line();
        lines = new ArrayList<>();
        changeListerners = new HashMap<>();
        linesToPointsStart = new HashMap<>();
        linesToPointsEnd = new HashMap<>();
        circles = new HashMap<>();
        circlesList = new ArrayList<>();
        linesToPointsStart.put(firstLine, startPointProperty);
        linesToPointsEnd.put(firstLine, endPointProperty);    
        addChangeLOnFirstsPoints();
        startPointProperty.set(new Point2D(startX, startY));
        endPointProperty.set(new Point2D(endX, endY));  
        tailProperty.addListener(new ChangeListener<Point2D>() {
            @Override
            public void changed(ObservableValue<? extends Point2D> observable, Point2D oldValue, Point2D newValue) {
                startPointProperty.set(newValue);
            }
        });  
        headProperty.addListener(new ChangeListener<Point2D>() {
            @Override
            public void changed(ObservableValue<? extends Point2D> observable, Point2D oldValue, Point2D newValue) {
                endPointProperty.set(newValue);
            }
        });
    }
        
// REQUESTS
    @Override
    public Line getFisrtLine() {
        return firstLine;
    }
    
    @Override
    public List<Line> getLines() {
        List<Line> linesRes = new ArrayList<>(lines);
        linesRes.add(firstLine);
        return linesRes;
    }
    
    @Override
    public List<Shape> getShapes() {
        List<Shape> l = new ArrayList<>();
        l.addAll(getLines());
        l.add(firstLine);
        l.addAll(circles.keySet());  
        return l;
    }
    
    @Override
    public List<Circle> getCircles() {
        return circlesList;
    }
    
    //Ajouter par @yan
    public Map<Circle, ObjectProperty<Point2D>> getCirclesMap(){
    	return this.circles;
    }
    
// COMMANDS
 
    //ajouter un point
    @Override
    public void addPoint(Point2D p) {
        Contract.check(p != null, "p must not be null.");
        
        ObjectProperty<Point2D> newPoint = new SimpleObjectProperty<>();
        Circle newCircle = new Circle(RADIUS, COLOR_DEFAULT);
        addMouseEventOnCircle(newCircle);
        newCircle.setVisible(false);
        circles.put(newCircle, newPoint);
        circlesList.add(newCircle);
        Line newLine = new Line();
        newLine.setStroke(colorLines);
        if (dash > 0) {
            newLine.getStrokeDashArray().add(dash);
        }
        Line lastLine;
        if (lines.isEmpty()) {
            lastLine = firstLine;
        } else {
            lastLine = lines.get(lines.size() - 1);
        }
        newLine.setEndX(endPointProperty.getValue().getX());
        newLine.setEndY(endPointProperty.getValue().getY());
        lines.add(newLine);
        
        ChangeListener<Point2D> newPointChangeL = new ChangeListener<Point2D>() {
            @Override
            public void changed(ObservableValue<? extends Point2D> observable, Point2D oldValue, Point2D newValue) {
                newLine.setStartX(newValue.getX());
                newLine.setStartY(newValue.getY());
                lastLine.setEndX(newValue.getX());
                lastLine.setEndY(newValue.getY());
                newCircle.setCenterX(newValue.getX());
                newCircle.setCenterY(newValue.getY());
            }
        };
        newPoint.addListener(newPointChangeL);
        changeListerners.put(newPoint, newPointChangeL);       
        endPointProperty.removeListener(changeListerners.get(endPointProperty));       
        ChangeListener<Point2D> lastPointChangeL = new ChangeListener<Point2D>() {
            @Override
            public void changed(ObservableValue<? extends Point2D> observable, Point2D oldValue, Point2D newValue) {
                newLine.setEndX(newValue.getX());
                newLine.setEndY(newValue.getY());
            }
        };
        endPointProperty.addListener(lastPointChangeL);
        changeListerners.put(endPointProperty, lastPointChangeL);      
        linesToPointsStart.put(newLine, newPoint);
        linesToPointsEnd.put(newLine, endPointProperty);
        linesToPointsEnd.put(lastLine, newPoint);       
        newPoint.setValue(p);
        List<Point2D> lst = new ArrayList<>();
        for(ObjectProperty<Point2D> pnt :circles.values()) {
        	lst.add(pnt.get());
        }
        Collections.sort(lst, new PointDistanceComparator(new Point2D(0,0)));
        int i =0;
        for(ObjectProperty<Point2D> pnt :circles.values()) {
        	pnt.set(lst.get(i));
        	i++;
        }
        
        
        
        
        
        
        
    }
    
    //supprimer un point
    public void removePoint(Point2D p) {
		Contract.check(p != null, "p must not be null.");
		if (circles.containsValue(p)) {
			Circle c = null;
			for (Map.Entry<Circle, ObjectProperty<Point2D>> entry : circles.entrySet()) {
				if (entry.getValue().getValue().equals(p)) {
					c = entry.getKey();
					break;
				}
			}
			if (c != null) {
				ObjectProperty<Point2D> point = circles.get(c);
				ChangeListener<Point2D> changeL = changeListerners.get(point);
				point.removeListener(changeL);
				circles.remove(c);
				circlesList.remove(c);
				c.setVisible(false);
				c.removeEventHandler(MouseEvent.MOUSE_ENTERED, null);
				c.removeEventHandler(MouseEvent.MOUSE_EXITED, null);
				c.removeEventFilter(MouseEvent.MOUSE_DRAGGED, null);
			}
		} else {
			Line l = null;
			for (Map.Entry<Line, ObjectProperty<Point2D>> entry : linesToPointsStart.entrySet()) {
				if (entry.getValue().getValue().equals(p)) {
					l = entry.getKey();
					break;
				}
			}
			if (l != null) {
				ObjectProperty<Point2D> point = linesToPointsStart.get(l);
				ChangeListener<Point2D> changeL = changeListerners.get(point);
				point.removeListener(changeL);
				linesToPointsStart.remove(l);
				linesToPointsEnd.remove(l);
				lines.remove(l);
			}
		}
    }
    
    //ajouter un point en le reliant aux points les plus proches
    @Override
    public void addPointWithProximity(Point2D p) {
    	        Contract.check(p != null, "p must not be null.");
    	        for (Line l : lines) {
    	            Point2D start = linesToPointsStart.get(l).getValue();
    	            Point2D end = linesToPointsEnd.get(l).getValue();
    	            if (start.distance(p) + p.distance(end) - start.distance(end) < 0.0001) {
    	                addPoint(p);
    	                circles.put(new Circle(p.getX(), p.getY(), RADIUS, COLOR_DEFAULT), null);
    	                
    	            }
    	        }
    }
    	        

    
    @Override
    public void clear() {
        clearAll();
        addChangeLOnFirstsPoints();      
        double startX = startPointProperty.get().getX();
        double startY = startPointProperty.get().getY();
        double endX = endPointProperty.get().getX();
        double endY = endPointProperty.get().getY();       
        startPointProperty.set(new Point2D(startX, startY));
        endPointProperty.set(new Point2D(endX, endY));
    }
    
     @Override
    public void clearAll() {
        circles.values().stream().forEach((point) -> {
            point.removeListener(changeListerners.get(point));
        });
        startPointProperty.removeListener(changeListerners.get(startPointProperty));
        endPointProperty.removeListener(changeListerners.get(endPointProperty));
        lines.clear();
        changeListerners.clear();
        linesToPointsStart.clear();
        linesToPointsEnd.clear();
        circles.clear();
        circlesList.clear();
    }
    
    @Override
    public void moveEndPoint(Point2D p) {
        Contract.check(p != null, "p must not be null.");
        endPointProperty.setValue(p);
    }
    
    @Override
    public void movePoint(Circle c, double x, double y) {
        Contract.check(c != null, "c must not be null.");
        ObjectProperty<Point2D> point = circles.get(c);
        point.set(new Point2D(x, y));
    }
    
    @Override
    public void moveStartPoint(Point2D p) {
        Contract.check(p != null, "p must not be null.");
        startPointProperty.setValue(p);
    }
    
     @Override
    public void setColor(Color c) {
        Contract.check(c != null, "c must not be null.");
        colorLines = c;
        firstLine.setStroke(c);
        getLines().stream().forEach((l) -> {
            l.setStroke(c);
        });
    }
    
    @Override
    public void setDashed(double dash) {
        this.dash = dash;
        getLines().stream().forEach((l) -> {
            if (dash > 0) {
                l.getStrokeDashArray().add(dash);
            } else {
                l.getStrokeDashArray().clear();
            }
        });
    }
       
    @Override
    public void setDNDPointEvent(EventHandler<MouseEvent> pointDnd_Event) {
        Contract.check(pointDnd_Event != null, "pointDnd_Event "
                + "must not be null.");
       this.pointDnd_Event = pointDnd_Event;
    }
    
    //Ajouter par #yanis
    public void addPropertyChangeListener(String name,PropertyChangeListener pcl) {
    	pcs.addPropertyChangeListener(pcl);
    }
    public void removePropertyChangeListener(String name, PropertyChangeListener pcl) {
    	pcs.removePropertyChangeListener(pcl);
    }
    public void firePropertyChanged(String name,Object old , Object newval) {
    	pcs.firePropertyChange(name, old, newval);
    }
    // fin 
    
// PRIVATE
    
    private void addChangeLOnFirstsPoints() {
         ChangeListener<Point2D> startPointChangeL = 
                new ChangeListener<Point2D>() {
                @Override
                public void changed(ObservableValue<? extends Point2D> observable, Point2D oldValue, Point2D newValue) {
                    firstLine.setStartX(newValue.getX());
                    firstLine.setStartY(newValue.getY());
                }
        };
        ChangeListener<Point2D> endPointChangeL = new ChangeListener<Point2D>() {
            @Override
            public void changed(ObservableValue<? extends Point2D> observable, Point2D oldValue, Point2D newValue) {
                firstLine.setEndX(newValue.getX());
                firstLine.setEndY(newValue.getY());
            }
        };
        startPointProperty.addListener(startPointChangeL);
        endPointProperty.addListener(endPointChangeL);
        changeListerners.put(startPointProperty, startPointChangeL);
        changeListerners.put(endPointProperty, endPointChangeL);
    }
    
    private void addMouseEventOnCircle(Circle c) {
        c.addEventHandler(MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                c.setVisible(true);
            }
        });      
        c.addEventHandler(MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                c.setVisible(false);
            }
        });            
        if (pointDnd_Event != null) {
            c.addEventFilter(MouseEvent.MOUSE_DRAGGED, pointDnd_Event);
        }
        
        c.addEventHandler(MouseEvent.MOUSE_CLICKED,new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				// TODO Auto-generated method stub
				if (event.getButton() == MouseButton.PRIMARY  && event.getClickCount() == 2) {
					 DialogRemovePoint dialog = new DialogRemovePoint();
					  ButtonType result = dialog.showAndWait();
					  if (result == ButtonType.YES) {


						  //Envoi de l'événement

						  firePropertyChanged(PROP_CIRCLE,null,c);

					  }
				}
				
				
				
			}
        	
        });

        
    }
    //Ajouter par @yanis
    public void addPoint2(Point2D p ,Point2D dst ) {
        Contract.check(p != null, "p must not be null.");
        
        ObjectProperty<Point2D> newPoint = new SimpleObjectProperty<>();
        Circle newCircle = new Circle(RADIUS, COLOR_DEFAULT);
        addMouseEventOnCircle(newCircle);
        newCircle.setVisible(false);
        circles.put(newCircle, newPoint);
        circlesList.add(newCircle);
        Line newLine = new Line();
        newLine.setStroke(colorLines);
        if (dash > 0) {
            newLine.getStrokeDashArray().add(dash);
        }
        Line lastLine;
        if (lines.isEmpty()) {
            lastLine = firstLine;
        } else {
            lastLine = lines.get(lines.size() - 1);
        }
        newLine.setEndX(endPointProperty.getValue().getX());
        newLine.setEndY(endPointProperty.getValue().getY());
        lines.add(newLine);
        
        ChangeListener<Point2D> newPointChangeL = new ChangeListener<Point2D>() {
            @Override
            public void changed(ObservableValue<? extends Point2D> observable, Point2D oldValue, Point2D newValue) {
                newLine.setStartX(newValue.getX());
                newLine.setStartY(newValue.getY());
                lastLine.setEndX(newValue.getX());
                lastLine.setEndY(newValue.getY());
                newCircle.setCenterX(newValue.getX());
                newCircle.setCenterY(newValue.getY());
            }
        };
        newPoint.addListener(newPointChangeL);
        changeListerners.put(newPoint, newPointChangeL);       
        endPointProperty.removeListener(changeListerners.get(endPointProperty));       
        ChangeListener<Point2D> lastPointChangeL = new ChangeListener<Point2D>() {
            @Override
            public void changed(ObservableValue<? extends Point2D> observable, Point2D oldValue, Point2D newValue) {
                newLine.setEndX(newValue.getX());
                newLine.setEndY(newValue.getY());
            }
        };
        endPointProperty.addListener(lastPointChangeL);
        changeListerners.put(endPointProperty, lastPointChangeL);      
        linesToPointsStart.put(newLine, newPoint);
        linesToPointsEnd.put(newLine, endPointProperty);
        linesToPointsEnd.put(lastLine, newPoint);       
        newPoint.setValue(p);
        List<Point2D> lst = new ArrayList<>();
        for(ObjectProperty<Point2D> pnt :circles.values()) {
        	lst.add(pnt.get());
        }
        Collections.sort(lst, new PointDistanceComparator(dst));
        int i =0;
        for(ObjectProperty<Point2D> pnt :circles.values()) {
        	pnt.set(lst.get(i));
        	System.out.println(lst.get(i));
        	i++;
        	
        }
        
    }
    // TYPE IMBRIQUE
    //@ajouter par @sadius
     class PointDistanceComparator implements Comparator<Point2D> {
        private final Point2D destination;

        public PointDistanceComparator(Point2D destination) {
            this.destination = destination;
        }

        @Override
        public int compare(Point2D point1, Point2D point2) {
            double distance1 = point1.distance(destination);
            double distance2 = point2.distance(destination);
            // Pour trier du plus loin au plus proche, nous comparons les distances dans l'ordre croissant
            return Double.compare(distance1, distance2);
        }
      }
}
