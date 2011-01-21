package jd.updater;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.appwork.utils.logging.Log;
import org.appwork.utils.zip.ZipIOException;

public class BranchCheck {

    public static void main(final String[] args) {
        final WebUpdaterOptions options = new WebUpdaterOptions();
        options.setGuiless(true);
        options.setBranch("last09");
        options.setWorkingDir(new File("c:/test/jdupdate2"));
        options.setIgnoreSlotWaittime(true);
        final UpdaterController updateController = new UpdaterController(options);
        try {
            updateController.checkHashLists();
        } catch (final InterruptedException e) {
            Log.exception(Level.WARNING, e);

        } catch (final UpdateException e) {
            Log.exception(Level.WARNING, e);

        } catch (final IOException e) {
            Log.exception(Level.WARNING, e);

        } catch (final ZipIOException e) {
            Log.exception(Level.WARNING, e);

        }

    }
}
