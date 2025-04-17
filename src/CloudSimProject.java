import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

public class CloudSimProject extends JFrame {
    private JTextField datacentersField, hostsField, hostMipsField, hostRamField, hostStorageField;
    private JTextField vmsField, vmMipsField, vmRamField, vmCostField;
    private JTextField budgetField;
    private JTextField cloudletsField, cloudletLengthField, fileSizeField, outputSizeField;
    private JComboBox<String> strategyBox;
    private JTextArea resultArea;
    private JPanel inputPanel;

    public CloudSimProject() {
        setTitle("Cost-Aware CloudSim Simulation");
        setSize(800, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize input panel
        inputPanel = new JPanel(new GridLayout(16, 2, 5, 5));
        initInputFields();

        // Initialize result area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane resultScrollPane = new JScrollPane(resultArea);

        // Add input panel and result area to main layout
        add(inputPanel, BorderLayout.NORTH);
        add(resultScrollPane, BorderLayout.CENTER);
    }

    private void initInputFields() {
        datacentersField = addLabeledField(inputPanel, "Number of Datacenters:");
        hostsField = addLabeledField(inputPanel, "Hosts per Datacenter:");
        hostMipsField = addLabeledField(inputPanel, "Host CPU MIPS:");
        hostRamField = addLabeledField(inputPanel, "Host RAM (MB):");
        hostStorageField = addLabeledField(inputPanel, "Host Storage (GB):");

        vmsField = addLabeledField(inputPanel, "VMs per Datacenter:");
        vmMipsField = addLabeledField(inputPanel, "VM CPU MIPS:");
        vmRamField = addLabeledField(inputPanel, "VM RAM (MB):");
        vmCostField = addLabeledField(inputPanel, "Cost per VM (INR):");

        budgetField = addLabeledField(inputPanel, "Total Budget (INR):");

        cloudletsField = addLabeledField(inputPanel, "Number of Cloudlets:");
        cloudletLengthField = addLabeledField(inputPanel, "Cloudlet Length:");
        fileSizeField = addLabeledField(inputPanel, "Cloudlet File Size (MB):");
        outputSizeField = addLabeledField(inputPanel, "Cloudlet Output Size (MB):");

        strategyBox = new JComboBox<>(new String[]{"TimeShared", "SpaceShared"});
        inputPanel.add(new JLabel("VM Scheduling Strategy:"));
        inputPanel.add(strategyBox);

        JButton runButton = new JButton("Run Simulation");
        runButton.addActionListener(this::runSimulation);
        inputPanel.add(runButton);
    }

    private JTextField addLabeledField(JPanel panel, String label) {
        JTextField field = new JTextField();
        panel.add(new JLabel(label));
        panel.add(field);
        return field;
    }

    private void runSimulation(ActionEvent e) {
        try {
            int numDatacenters = Integer.parseInt(datacentersField.getText());
            int hostsPerDC = Integer.parseInt(hostsField.getText());
            int hostMips = Integer.parseInt(hostMipsField.getText());
            int hostRam = Integer.parseInt(hostRamField.getText());
            long hostStorage = Long.parseLong(hostStorageField.getText()) * 1024;

            int vmsPerDC = Integer.parseInt(vmsField.getText());
            int vmMips = Integer.parseInt(vmMipsField.getText());
            int vmRam = Integer.parseInt(vmRamField.getText());
            double vmCost = Double.parseDouble(vmCostField.getText());

            double budget = Double.parseDouble(budgetField.getText());
            int totalVMs = vmsPerDC * numDatacenters;
            double totalCost = totalVMs * vmCost;

            String budgetMsg = (budget >= totalCost)
                    ? "✅ Budget is sufficient. Total cost: " + totalCost + " INR\nRemaining: " + (budget - totalCost) + " INR"
                    : "❌ Budget insufficient. Required: " + totalCost + " INR\nPlease add more " + (totalCost - budget) + " INR.";

            int numCloudlets = Integer.parseInt(cloudletsField.getText());
            int cloudletLength = Integer.parseInt(cloudletLengthField.getText());
            int fileSize = Integer.parseInt(fileSizeField.getText());
            int outputSize = Integer.parseInt(outputSizeField.getText());
            String strategy = (String) strategyBox.getSelectedItem();

            CloudSim.init(1, Calendar.getInstance(), false);
            DatacenterBroker broker = new DatacenterBroker("Broker_1");
            int brokerId = broker.getId();

            List<Datacenter> datacenterList = new ArrayList<>();
            for (int i = 0; i < numDatacenters; i++) {
                datacenterList.add(createDatacenter("Datacenter_" + i, hostsPerDC, hostMips, hostRam, hostStorage));
            }

            List<Vm> vmList = new ArrayList<>();
            for (int i = 0; i < totalVMs; i++) {
                Vm vm = new Vm(i, brokerId, vmMips, 1, vmRam, 10000, 10000, "Xen",
                        "TimeShared".equals(strategy) ? new CloudletSchedulerTimeShared() : new CloudletSchedulerSpaceShared());
                vmList.add(vm);
            }

            List<Cloudlet> cloudletList = new ArrayList<>();
            UtilizationModel utilization = new UtilizationModelFull();
            for (int i = 0; i < numCloudlets; i++) {
                Cloudlet cl = new Cloudlet(i, cloudletLength, 1, fileSize, outputSize, utilization, utilization, utilization);
                cl.setUserId(brokerId);
                cloudletList.add(cl);
            }

            broker.submitVmList(vmList);
            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();
            CloudSim.stopSimulation();

            List<Cloudlet> resultList = broker.getCloudletReceivedList();
            displayResults(resultList, totalCost, budgetMsg);

        } catch (Exception ex) {
            resultArea.setText("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Datacenter createDatacenter(String name, int hostsCount, int mips, int ram, long storage) throws Exception {
        List<Host> hostList = new ArrayList<>();
        for (int i = 0; i < hostsCount; i++) {
            List<Pe> peList = new ArrayList<>();
            peList.add(new Pe(0, new PeProvisionerSimple(mips)));

            hostList.add(new Host(i, new RamProvisionerSimple(ram), new BwProvisionerSimple(10000), storage, peList, new VmSchedulerTimeShared(peList)));
        }
        String arch = "x86", os = "Linux", vmm = "Xen";
        double timeZone = 10.0, costPerSec = 3.0, costPerMem = 0.05, costPerStorage = 0.001, costPerBw = 0.0;
        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hostList, timeZone, costPerSec, costPerMem, costPerStorage, costPerBw);
        return new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), new LinkedList<>(), 0);
    }

    private void displayResults(List<Cloudlet> list, double totalCost, String budgetMsg) {
        StringBuilder sb = new StringBuilder("\n========== CLOUDLET EXECUTION RESULT ==========\n");
        sb.append("Broker Information:\n");
        sb.append("Broker Name: Broker_1\n");
        sb.append("Broker ID  : 1\n\n");

        sb.append("Cloudlet ID    Status    VM ID    Broker ID    Time    Start    Finish\n");
        sb.append("---------------------------------------------------------------------");
        for (Cloudlet c : list) {
            sb.append(String.format("\n%-13d%-10s%-9d%-13d%-8.1f%-8.1f%-8.1f",
                    c.getCloudletId(),
                    (c.getStatus() == Cloudlet.SUCCESS ? "SUCCESS" : "FAILED"),
                    c.getVmId(),
                    c.getUserId(),
                    c.getActualCPUTime(),
                    c.getExecStartTime(),
                    c.getFinishTime()));
        }

        sb.append("\n\nTotal Cost of VM Allocation: ").append(totalCost).append(" INR\n");
        sb.append(budgetMsg);
        resultArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CloudSimProject().setVisible(true));
    }
}