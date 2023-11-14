package aqua.blatt1.client;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;

public class AquaGui extends JFrame implements Runnable, Observer {
    private final List<JMenuItem> fishMenuItems = Collections
            .synchronizedList(new ArrayList<>());

    private final JMenu searchMenu;
    private final Runnable updateRunnable;

    public AquaGui(final TankModel tankModel) {
        TankView tankView = new TankView(tankModel);
        tankModel.addObserver(tankView);
        add(tankView);

        pack();

        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                tankModel.finish();
                System.exit(0);
            }
        });

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu toolsMenu = new JMenu("Tools");
        menuBar.add(toolsMenu);

        JMenuItem gsMenuItem = new JMenuItem("Global Snapshot");
        toolsMenu.add(gsMenuItem);

        gsMenuItem.addActionListener(new SnapshotController(tankModel));

        searchMenu = new JMenu("Toggle Fish Color...");
        toolsMenu.add(searchMenu);
        tankModel.addObserver(this);

        updateRunnable = () -> {
            setTitle(tankModel.getId());

            int size = fishMenuItems.size();
            while (tankModel.getFishCounter() > size) {
                String fishId = "fish" + (++size) + "@" + tankModel.getId();
                JMenuItem fishMenuItem = new JMenuItem(fishId);
                fishMenuItem.addActionListener(new ToggleController(tankModel, fishId));
                fishMenuItems.add(fishMenuItem);
                searchMenu.add(fishMenuItem);
            }
        };
    }

    @Override
    public void run() {
        setVisible(true);
    }

    @Override
    public void update(Observable o, Object arg) {
        SwingUtilities.invokeLater(updateRunnable);
    }

}
