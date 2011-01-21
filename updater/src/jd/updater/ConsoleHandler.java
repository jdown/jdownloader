package jd.updater;

public class ConsoleHandler implements UpdaterListener {

    private final UpdaterController updateController;

    public ConsoleHandler(final UpdaterController updateController) {
        this.updateController = updateController;
    }

    @Override
    public void onUpdaterEvent(final UpdaterEvent event) {
        System.out.println("Updater: " + event);
        switch (event.getType()) {
        case WAIT_PENALTY:
            ((Throwable) event.getParameter(1)).printStackTrace();
            break;
        }

    }

    @Override
    public void onUpdaterModuleEnd(final UpdaterEvent event) {
        System.out.println("Finished: " + event.getType());
        switch (event.getType()) {
        case END_FILELIST_UPDATE:
            System.out.println("Files in List: " + (updateController.getNonClassFiles().size() + updateController.getClassFiles().size()));
            break;
        case END_FILTERING:
            System.out.println(updateController.getFilteredClassFiles() + " - " + updateController.getFilteredNonClassFiles());
            break;
        }

    }

    @Override
    public void onUpdaterModuleProgress(final UpdaterEvent event, final int parameter) {
        System.out.println("Progress: " + event.getType() + " : " + parameter);
    }

    @Override
    public void onUpdaterModuleStart(final UpdaterEvent event) {
        System.out.println("Started: " + event.getType());

    }

}
