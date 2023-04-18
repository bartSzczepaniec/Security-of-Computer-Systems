package pg.projekt;

import org.checkerframework.checker.units.qual.A;
import pg.projekt.sockets.applogic.AppLogic;

import javax.swing.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        AppLogic backend = new AppLogic();
        AppGUI appGUI = new AppGUI(backend);
        appGUI.startApp();
    }
}
