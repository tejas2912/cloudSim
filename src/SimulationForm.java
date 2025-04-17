import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class SimulationForm {
    public static void main(String[] args) {
        int i = 1;
        JFrame frame = new JFrame("CloudSim Budget Simulation Form");
        frame.setSize(500, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(0, 2, 10, 5));

        String[] labels = {
                "No. of Datacenters", "Hosts per DC", "Host CPU MIPS", "Host RAM (MB)", "Host Storage (GB)",
                "VMs per DC", "VM CPU MIPS", "VM RAM (MB)", "Cost per VM (INR)", "Total Budget (INR)",
                "No. of Cloudlets", "Cloudlet Length", "File Size (MB)", "Output Size (MB)",
                "Scheduling Strategy (TimeShared/SpaceShared)"
        };

        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            frame.add(new JLabel(labels[i]));
            fields[i] = new JTextField();
            frame.add(fields[i]);
        }

        frame.add(new JLabel("Notes / Observations:"));
        JTextArea comments = new JTextArea(5, 30);
        frame.add(new JScrollPane(comments));

        JButton submit = new JButton("Submit");
        frame.add(submit);

        JTextArea output = new JTextArea(10, 40);
        output.setEditable(false);
        frame.add(new JScrollPane(output));

        submit.addActionListener(e -> {
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < labels.length; i++) {
                    sb.append(labels[i]).append(": ").append(fields[i].getText()).append("\n");
                }
                sb.append("User Comments:\n").append(comments.getText()).append("\n");
                output.setText(sb.toString());
                JOptionPane.showMessageDialog(frame, "Inputs received successfully!");
                // Optionally: Parse and call CloudSim simulator here.
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        frame.setVisible(true);
    }

}
