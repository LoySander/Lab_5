package com.content5;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPanel;

public class GraphicsDisplay extends JPanel {
    // массив точек
    private ArrayList<Double[]> graphicsData;

    private ArrayList<Double[]> originalData;
    private int selectedMarker = -1;
    // границы диапозона пространства
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double[][] viewport = new double[2][2];
    private ArrayList<double[][]> undoHistory = new ArrayList(5);
    // масштаб
    private double scaleX;
    private double scaleY;
    // стили для чертения линий
    private BasicStroke axisStroke;
    private BasicStroke gridStroke;
    private BasicStroke markerStroke;
    private BasicStroke selectionStroke;
    private BasicStroke rectangleStroke;
    // шрифты к подписям осей и числовых значений на осях
    private Font axisFont;
    private Font labelsFont;
    private static DecimalFormat formatter = (DecimalFormat)NumberFormat.getInstance();
    // флаги
    private boolean rectangleMode = false;
    private boolean scaleMode = false; // приближение
    private boolean changeMode = false;
    // точка
    private double[] originalPoint = new double[2];
    // Фигура, представляющая прямоугольник
    private java.awt.geom.Rectangle2D.Double selectionRect = new java.awt.geom.Rectangle2D.Double();
//инциализация  повторно используемых объектов, представляющих стили черчения линий и шрифт написания
    public GraphicsDisplay() {
        // фон
        this.setBackground(Color.WHITE);
        // перо для рисования осей
        this.axisStroke = new BasicStroke(2.0F, 0, 0, 10.0F, (float[])null, 0.0F);
        // перо для рисования сетки
        this.gridStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[]{4.0F, 4.0F}, 0.0F);
        // перо для рисования точек(маркеров)
        this.markerStroke = new BasicStroke(1.0F, 0, 0, 10.0F, (float[])null, 0.0F);
        this.selectionStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[]{10.0F, 10.0F}, 0.0F);
        this.rectangleStroke = new BasicStroke(4.0F, 0, 0, 10.0F, new float[]{4.0F, 4.0F}, 0.0F);
        // Шрифт для подписей осей координат
        this.axisFont = new Font("Serif", 1, 36);
        // шрифт для подписей числовых значений на осях
        this.labelsFont = new Font("Serif", 0, 10);
        formatter.setMaximumFractionDigits(5); // устанавливает максимальное количество цифр после десятичной точки в форматируемом объекте
        this.addMouseListener(new GraphicsDisplay.MouseHandler()); // ?
        this.addMouseMotionListener(new GraphicsDisplay.MouseMotionHandler()); // ?
    }
    //Данный метод вызывается из обработчика элемента меню "Открыть файл с графиком"
    // главного окна приложения в случае успешной загрузки данных
    // перерисовка
    public void displayGraphics(ArrayList<Double[]> graphicsData) {
        this.graphicsData = graphicsData;
        // список размера graphicsData
        this.originalData = new ArrayList(graphicsData.size());
        Iterator var3 = graphicsData.iterator();
 // с помощью hasNext можно узнать, если следующий элемент
        while(var3.hasNext()) {
           //С помощью метода hasNext() можно узнать, есть ли следующий элемент, и не достигнут ли конец
            Double[] point = (Double[])var3.next(); // получаем точку
            Double[] newPoint = new Double[]{new Double(point[0]), new Double(point[1])}; // тоже самое что double n[2] = {x,y}
            this.originalData.add(newPoint);
        }
        // получаем максмальные знаечния для y и
        this.minX = ((Double[])graphicsData.get(0))[0]; // возвращаю double[0] в котором x и y и беру [0] - x
        this.maxX = ((Double[])graphicsData.get(graphicsData.size() - 1))[0];
        this.minY = ((Double[])graphicsData.get(0))[1];
        this.maxY = this.minY;

        for(int i = 1; i < graphicsData.size(); ++i) {
            if (((Double[])graphicsData.get(i))[1] < this.minY) {
                this.minY = ((Double[])graphicsData.get(i))[1];
            }

            if (((Double[])graphicsData.get(i))[1] > this.maxY) {
                this.maxY = ((Double[])graphicsData.get(i))[1];
            }
        }

        this.zoomToRegion(this.minX, this.maxY, this.maxX, this.minY);
    }
 // метод для приближения
    public void zoomToRegion(double x1, double y1, double x2, double y2) {
        this.viewport[0][0] = x1;
        this.viewport[0][1] = y1;
        this.viewport[1][0] = x2;
        this.viewport[1][1] = y2;
        this.repaint();
    }
 //  пределение  коэффициента  масштаба  и  границ отображаемой области пространства, сохранение и восстановление настроек рисования,
 //  а также вызов (по необходимости) методов, осуществляющих отображение элементов графика
    public void paintComponent(Graphics g) {
        //Вызвать метод предка для заливки области цветом заднего фона* Эта функциональность -единственное, что осталось в наследство от * paintComponentкласса JPane
        super.paintComponent(g);
        // масштабирования если знаем viewport
        this.scaleX = this.getSize().getWidth() / (this.viewport[1][0] - this.viewport[0][0]);
        this.scaleY = this.getSize().getHeight() / (this.viewport[0][1] - this.viewport[1][1]);
        //Если данные графика не загружены (при показе компонента при // запуске программы) -ничего не делать
        if (this.graphicsData != null && this.graphicsData.size() != 0) {
            //Преобразовать экземпляр Graphics к Graphics2D
            Graphics2D canvas = (Graphics2D)g;
            // отрисовать сетку
            this.paintGrid(canvas);
            //отрисовываются оси координат
            this.paintAxis(canvas);
            // отрисовать график
            this.paintGraphics(canvas);
            // маркера
            this.paintMarkers(canvas);
            // подписи
            this.paintLabels(canvas);
            this.paintRectangle(canvas);
            this.paintSelection(canvas);
        }
    }
    private void paintRectangle(Graphics2D canvas){
        if(this.rectangleMode){
            canvas.setStroke(this.rectangleStroke);
            canvas.setColor(Color.RED);
            canvas.draw(this.selectionRect);
        }
    }

    private void paintSelection(Graphics2D canvas) {
        if (this.scaleMode) {
            canvas.setStroke(this.selectionStroke);
            canvas.setColor(Color.BLACK);
            canvas.draw(this.selectionRect);
        }
    }
   //  Отрисовка графика по прочитанным координатам
    private void paintGraphics(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.RED);
        Double currentX = null;
        Double currentY = null;
        Iterator var5 = this.graphicsData.iterator();
         // должны нарисовать график
        while(var5.hasNext()) {
            Double[] point = (Double[])var5.next();
            if (point[0] >= this.viewport[0][0] && point[1] <= this.viewport[0][1] && point[0] <= this.viewport[1][0] && point[1] >= this.viewport[1][1]) {
                // если текущий икс и игрик ноль то ставим "перо в точку", а дальше пойдем рисовать от нее
                if (currentX != null && currentY != null) {
                    canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(currentX, currentY), this.translateXYtoPoint(point[0], point[1])));
                }

                currentX = point[0];
                currentY = point[1];
            }
        }

    }
   // метод для отображения точек графика(маркеров)
    private void paintMarkers(Graphics2D canvas) {
        // Установить специальное перо для черчения контуров маркеров
        canvas.setStroke(this.markerStroke);
        // Выбрать красный цвета для контуров маркеров
        canvas.setColor(Color.RED);
        // Выбрать красный цвет для закрашивания маркеров внутри
        canvas.setPaint(Color.RED);
        java.awt.geom.Ellipse2D.Double lastMarker = null;
        int i = -1;
        Iterator var5 = this.graphicsData.iterator();
   // Организовать цикл по всем точкам графика
        while(var5.hasNext()) {
            Double[] point = (Double[])var5.next();
            ++i; // ?
            if (point[0] >= this.viewport[0][0] && point[1] <= this.viewport[0][1] && point[0] <= this.viewport[1][0] && point[1] >= this.viewport[1][1]) {
                byte radius;
                if (i == this.selectedMarker) {
                    radius = 6;
                } else {
                    radius = 3;
                }
                // Инициализировать эллипс как объект для представления маркера
                java.awt.geom.Ellipse2D.Double marker = new java.awt.geom.Ellipse2D.Double();
                // Эллипс будет задаваться посредством указания координат его центра и угла прямоугольника, в который он вписан
                // центр - в точке (x,y)
                Point2D center = this.translateXYtoPoint(point[0], point[1]);
                // Угол прямоугольника
                Point2D corner = new java.awt.geom.Point2D.Double(center.getX() + (double)radius, center.getY() + (double)radius);
                // Задать эллипс по центру и диагонали
                marker.setFrameFromCenter(center, corner);
                if (i == this.selectedMarker) {
                    lastMarker = marker;
                } else {
                    // контур
                    canvas.draw(marker);
                    // залить
                    canvas.fill(marker);
                }
            }
        }

        if (lastMarker != null) {
            canvas.setColor(Color.BLUE);
            canvas.setPaint(Color.BLUE);
            canvas.draw(lastMarker);
            canvas.fill(lastMarker);
        }

    }
    // отрисовка значений осей
    private void paintLabels(Graphics2D canvas) {
        // выбрать цвет для подписей
        canvas.setColor(Color.BLACK);
        // Подписи  делаются специальным шрифтом
        canvas.setFont(this.labelsFont);
        // Создать объект контекста отображения текста -для получения
        // характеристик устройства (экрана)
        FontRenderContext context = canvas.getFontRenderContext();
        // позиция по y
        double labelYPos;
        // y2 < 0 и y1> 0
        if (this.viewport[1][1] < 0.0D && this.viewport[0][1] > 0.0D) {
            labelYPos = 0.0D;
        } else {
            labelYPos = this.viewport[1][1];
        }
        // позиция по x
        double labelXPos;
        // x1 < 0 и x2> 0
        if (this.viewport[0][0] < 0.0D && this.viewport[1][0] > 0.0D) {
            labelXPos = 0.0D;
        } else {
            labelXPos = this.viewport[0][0];
        }

        double pos = this.viewport[0][0];
        // шаг
        double step;
        // Представляет  точку,  с  координатами (x,y). Используется  для задания фигур, но сам фигурой не является.
        java.awt.geom.Point2D.Double point;
        String label;
        // граница
        Rectangle2D bounds;
        for(step = (this.viewport[1][0] - this.viewport[0][0]) / 10.0D; pos < this.viewport[1][0]; pos += step) {
            point = this.translateXYtoPoint(pos, labelYPos);
            label = formatter.format(pos);
            //  Возвращает границы указанного String в указанном Graphics контекст
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
        }

        pos = this.viewport[1][1];

        for(step = (this.viewport[0][1] - this.viewport[1][1]) / 10.0D; pos < this.viewport[0][1]; pos += step) {
            point = this.translateXYtoPoint(labelXPos, pos);
            label = formatter.format(pos);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
        }

        if (this.selectedMarker >= 0) {
            point = this.translateXYtoPoint(((Double[])this.graphicsData.get(this.selectedMarker))[0], ((Double[])this.graphicsData.get(this.selectedMarker))[1]);
            label = "X=" + formatter.format(((Double[])this.graphicsData.get(this.selectedMarker))[0]) + ", Y=" + formatter.format(((Double[])this.graphicsData.get(this.selectedMarker))[1]);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.setColor(Color.BLUE);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
        }

    }
 // отображение сетки
    private void paintGrid(Graphics2D canvas) {
        canvas.setStroke(this.gridStroke);
        canvas.setColor(Color.GRAY);
        double pos = this.viewport[0][0];

        double step;
        for(step = (this.viewport[1][0] - this.viewport[0][0]) / 10.0D; pos < this.viewport[1][0]; pos += step) {
            canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(pos, this.viewport[0][1]), this.translateXYtoPoint(pos, this.viewport[1][1])));
        }

        canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(this.viewport[1][0], this.viewport[0][1]), this.translateXYtoPoint(this.viewport[1][0], this.viewport[1][1])));
        pos = this.viewport[1][1];

        for(step = (this.viewport[0][1] - this.viewport[1][1]) / 10.0D; pos < this.viewport[0][1]; pos += step) {
            canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], pos), this.translateXYtoPoint(this.viewport[1][0], pos)));
        }

        canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], this.viewport[0][1]), this.translateXYtoPoint(this.viewport[1][0], this.viewport[0][1])));
    }
    // отображение подписей осей
    private void paintAxis(Graphics2D canvas) {
        // Установить особое начертание для осей
        canvas.setStroke(this.axisStroke);
        canvas.setColor(Color.BLACK);
        // шрифт
        canvas.setFont(this.axisFont);
        // Создать объект контекста отображения текста -для получения
        // характеристик устройства (экрана)
        FontRenderContext context = canvas.getFontRenderContext();
        Rectangle2D bounds;
        java.awt.geom.Point2D.Double labelPos;
        if (this.viewport[0][0] <= 0.0D && this.viewport[1][0] >= 0.0D) {
            canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(0.0D, this.viewport[0][1]), this.translateXYtoPoint(0.0D, this.viewport[1][1])));
            canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(-(this.viewport[1][0] - this.viewport[0][0]) * 0.0025D, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015D), this.translateXYtoPoint(0.0D, this.viewport[0][1])));
            canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint((this.viewport[1][0] - this.viewport[0][0]) * 0.0025D, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015D), this.translateXYtoPoint(0.0D, this.viewport[0][1])));
            bounds = this.axisFont.getStringBounds("y", context);
            labelPos = this.translateXYtoPoint(0.0D, this.viewport[0][1]);
            canvas.drawString("y", (float)labelPos.x + 10.0F, (float)(labelPos.y + bounds.getHeight() / 2.0D));
        }

        if (this.viewport[1][1] <= 0.0D && this.viewport[0][1] >= 0.0D) {
            canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], 0.0D), this.translateXYtoPoint(this.viewport[1][0], 0.0D)));
            canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.01D, (this.viewport[0][1] - this.viewport[1][1]) * 0.005D), this.translateXYtoPoint(this.viewport[1][0], 0.0D)));
            canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.01D, -(this.viewport[0][1] - this.viewport[1][1]) * 0.005D), this.translateXYtoPoint(this.viewport[1][0], 0.0D)));
            bounds = this.axisFont.getStringBounds("x", context);
            labelPos = this.translateXYtoPoint(this.viewport[1][0], 0.0D);
            canvas.drawString("x", (float)(labelPos.x - bounds.getWidth() - 10.0D), (float)(labelPos.y - bounds.getHeight() / 2.0D));
        }

    }

    protected java.awt.geom.Point2D.Double translateXYtoPoint(double x, double y) {
        // Вычисляем смещение Xот самой левой точки (minX)
        double deltaX = x - this.viewport[0][0];
        // Вычисляем смещение Yот точки верхней точки (maxY)
        double deltaY = this.viewport[0][1] - y;
        return new java.awt.geom.Point2D.Double(deltaX * this.scaleX, deltaY * this.scaleY);
    }
        // ?
    protected double[] translatePointToXY(int x, int y) {
        return new double[]{this.viewport[0][0] + (double)x / this.scaleX, this.viewport[0][1] - (double)y / this.scaleY};
    }
   // найти точку
    protected int findSelectedPoint(int x, int y) {
        if (this.graphicsData == null) {
            return -1;
        } else {
            int pos = 0;

            for(Iterator var5 = this.graphicsData.iterator(); var5.hasNext(); ++pos) {
                Double[] point = (Double[])var5.next();
                java.awt.geom.Point2D.Double screenPoint = this.translateXYtoPoint(point[0], point[1]);
                double distance = (screenPoint.getX() - (double)x) * (screenPoint.getX() - (double)x) + (screenPoint.getY() - (double)y) * (screenPoint.getY() - (double)y);
                if (distance < 100.0D) {
                    return pos;
                }
            }

            return -1;
        }
    }
  // восстановить график по умолчанию
    public void reset() {
        this.displayGraphics(this.originalData);
    }

    public class MouseHandler extends MouseAdapter {
        public MouseHandler() {
        }

        public void mouseClicked(MouseEvent ev) {
            if (ev.getButton() == 3) {
                if (GraphicsDisplay.this.undoHistory.size() > 0) {
                    GraphicsDisplay.this.viewport = (double[][])GraphicsDisplay.this.undoHistory.get(GraphicsDisplay.this.undoHistory.size() - 1);
                    GraphicsDisplay.this.undoHistory.remove(GraphicsDisplay.this.undoHistory.size() - 1);
                } else {
                    GraphicsDisplay.this.zoomToRegion(GraphicsDisplay.this.minX, GraphicsDisplay.this.maxY, GraphicsDisplay.this.maxX, GraphicsDisplay.this.minY);
                }

                GraphicsDisplay.this.repaint();
            }

        }

        public void mousePressed(MouseEvent ev) {
            if (ev.getButton() == 1) {
                GraphicsDisplay.this.selectedMarker = GraphicsDisplay.this.findSelectedPoint(ev.getX(), ev.getY());
                GraphicsDisplay.this.originalPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
                if (GraphicsDisplay.this.selectedMarker >= 0) {
                    GraphicsDisplay.this.changeMode = true;
                    GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(8));
                }



                else {
                    GraphicsDisplay.this.scaleMode = true;
                    GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(5));
                    GraphicsDisplay.this.selectionRect.setFrame((double)ev.getX(), (double)ev.getY(), 1.0D, 1.0D);
                }


            }
            if(ev.getButton() == 3){
                GraphicsDisplay.this.rectangleMode = true;
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(5));
                GraphicsDisplay.this.selectionRect.setFrame((double)ev.getX()-25, (double)ev.getY()-25, 50, 50);
                GraphicsDisplay.this.repaint();
            }
        }

        public void mouseReleased(MouseEvent ev) {
            if (ev.getButton() == 1) {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(0));
                if (GraphicsDisplay.this.changeMode) {
                    GraphicsDisplay.this.changeMode = false;
                } else {
                    GraphicsDisplay.this.scaleMode = false;
                    double[] finalPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
                    GraphicsDisplay.this.undoHistory.add(GraphicsDisplay.this.viewport);
                    GraphicsDisplay.this.viewport = new double[2][2];
                    GraphicsDisplay.this.zoomToRegion(GraphicsDisplay.this.originalPoint[0], GraphicsDisplay.this.originalPoint[1], finalPoint[0], finalPoint[1]);
                    GraphicsDisplay.this.repaint();
                }

            }
            if (ev.getButton() == 3){
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(0));
                GraphicsDisplay.this.rectangleMode = false;
                GraphicsDisplay.this.reset();
            }
        }
    }

    public class MouseMotionHandler implements MouseMotionListener {
        public MouseMotionHandler() {
        }

        public void mouseMoved(MouseEvent ev) {
            GraphicsDisplay.this.selectedMarker = GraphicsDisplay.this.findSelectedPoint(ev.getX(), ev.getY());
            if (GraphicsDisplay.this.selectedMarker >= 0) {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(8));
            } else {
                GraphicsDisplay.this.setCursor(Cursor.getPredefinedCursor(0));
            }

            GraphicsDisplay.this.repaint();
        }

        public void mouseDragged(MouseEvent ev) {
            if (GraphicsDisplay.this.changeMode) {
                double[] currentPoint = GraphicsDisplay.this.translatePointToXY(ev.getX(), ev.getY());
                double newY = ((Double[])GraphicsDisplay.this.graphicsData.get(GraphicsDisplay.this.selectedMarker))[1] + (currentPoint[1] - ((Double[])GraphicsDisplay.this.graphicsData.get(GraphicsDisplay.this.selectedMarker))[1]);
                if (newY > GraphicsDisplay.this.viewport[0][1]) {
                    newY = GraphicsDisplay.this.viewport[0][1];
                }

                if (newY < GraphicsDisplay.this.viewport[1][1]) {
                    newY = GraphicsDisplay.this.viewport[1][1];
                }

                ((Double[])GraphicsDisplay.this.graphicsData.get(GraphicsDisplay.this.selectedMarker))[1] = newY;
                GraphicsDisplay.this.repaint();
            } if((GraphicsDisplay.this.scaleMode))  {
                double width = (double)ev.getX() - GraphicsDisplay.this.selectionRect.getX();
                if (width < 5.0D) {
                    width = 5.0D;
                }

                double height = (double)ev.getY() - GraphicsDisplay.this.selectionRect.getY();
                if (height < 5.0D) {
                    height = 5.0D;
                }

                GraphicsDisplay.this.selectionRect.setFrame(GraphicsDisplay.this.selectionRect.getX(), GraphicsDisplay.this.selectionRect.getY(), width, height);
                GraphicsDisplay.this.repaint();
            }

        }
    }
}