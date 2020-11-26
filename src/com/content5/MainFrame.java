package com.content5;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class MainFrame extends JFrame {
    // размеры окна приложения
    private static final int WIDTH = 700;
    private static final int HEIGHT = 500;
    // объект диалогового окна для выбора файлов
    private JFileChooser fileChooser = null;
     // объект мень "сбросить настройки"
    private JMenuItem resetGraphicsMenuItem;
    // компонент отображения
    private GraphicsDisplay display = new GraphicsDisplay();
    // флаг, указывающий на загруженность файла
    private boolean fileLoaded = false;

    public MainFrame() {
        // вызов конструктора предка
        super("Обработка событий от мыши");
        // размеры окна
        this.setSize(700, 500);
        Toolkit kit = Toolkit.getDefaultToolkit();
        // отцентрировать окно
        this.setLocation((kit.getScreenSize().width - 700) / 2, (kit.getScreenSize().height - 500) / 2);
        //развертывание окна(//this.setExtendedState(MAXIMIZED_BOTH)
        this.setExtendedState(6);
        // полоса меню
        JMenuBar menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);
        //пункт меню файл
        JMenu fileMenu = new JMenu("Файл");
        menuBar.add(fileMenu);
        // создать действие по открытию файла
        Action openGraphicsAction = new AbstractAction("Открыть файл с графиком") {
            public void actionPerformed(ActionEvent event) {
                if (MainFrame.this.fileChooser == null) {
                    MainFrame.this.fileChooser = new JFileChooser();
                    MainFrame.this.fileChooser.setCurrentDirectory(new File("."));
                }

                MainFrame.this.fileChooser.showOpenDialog(MainFrame.this);
                MainFrame.this.openGraphics(MainFrame.this.fileChooser.getSelectedFile());
            }
        };
        //  Добавить соответствующий элемент меню
        fileMenu.add(openGraphicsAction);
        // создать действие отмена
        Action resetGraphicsAction = new AbstractAction("Отменить все изменения") {
            public void actionPerformed(ActionEvent event) {
                MainFrame.this.display.reset();
            }
        };
        this.resetGraphicsMenuItem = fileMenu.add(resetGraphicsAction);
        this.resetGraphicsMenuItem.setEnabled(false);
        // Установить GraphicsDisplay в центр граничной компоновки
        this.getContentPane().add(this.display, "Center");
    }
    // Считывание данных графика из существующего файла
    protected void openGraphics(File selectedFile) {
        try {
            // открыть поток для чтения
            DataInputStream in = new DataInputStream(new FileInputStream(selectedFile));
            // зарезервировать объем памяти ( автоматически расширяемый массив)
            ArrayList graphicsData = new ArrayList(50);
            // цикл чтения
            while(in.available() > 0) {
                // Первой из потока читается координата точки X
                Double x = in.readDouble();
                // второй читается Y
                Double y = in.readDouble();
                // добавляем в массив
                graphicsData.add(new Double[]{x, y});
            }

            if (graphicsData.size() > 0 && graphicsData!=null) {
                this.fileLoaded = true;
                this.resetGraphicsMenuItem.setEnabled(true);
                this.display.displayGraphics(graphicsData);
            }

        } catch (FileNotFoundException var6) {
            JOptionPane.showMessageDialog(this, "Указанный файл не найден", "Ошибка загрузки данных", 2);
        } catch (IOException var7) {
            JOptionPane.showMessageDialog(this, "Ошибка чтения координат точек из файла", "Ошибка загрузки данных", 2);
        }
    }

    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setDefaultCloseOperation(3);
        frame.setVisible(true);
    }
}
