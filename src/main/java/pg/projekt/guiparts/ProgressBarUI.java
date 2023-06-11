package pg.projekt.guiparts;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

@Getter
@Setter
public class ProgressBarUI {
    private JFrame frame;
    private JPanel mainPanel;
    private JProgressBar progressBar;
    private JLabel titleLabel;
    private JLabel progressLabel;
    private JButton continueButton;

    private long bytesTotal;
    private long bytes;
    private boolean receiving;
    private String fileName;

    public ProgressBarUI(long bytesTotal, String fileName, boolean receiving) {
        this.bytesTotal = bytesTotal;
        this.bytes = 0;
        this.fileName = fileName;
        this.receiving = receiving;
        if(receiving)
            frame = new JFrame("Receiving file progress");
        else
            frame = new JFrame("Sending file progress");
    }

    public void startProgressBar() {
        progressLabel.setText("Size of file:" + bytesTotal + " bytes");
        titleLabel.setText(fileName);

        continueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });

        frame.add(mainPanel);
        frame.setResizable(false);
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);


    }

    public void updateProgress(long bytesLeft) {
        long progress = bytesTotal - bytesLeft;
        progressBar.setValue((int) ((100 * progress) / bytesTotal));
        //progressBar.updateUI();

        if(bytesLeft == 0) {
            continueButton.setEnabled(true);
        }

    }
}
