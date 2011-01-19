package jd.updater;

import java.util.ArrayList;

import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.logging.Log;

public class BranchController {

    public static boolean isBetaBranch(final String string) {
        return string.trim().startsWith("beta_");
    }

    private final UpdaterController updaterController;
    private final Browser           br;

    private final ArrayList<String> stableBranches;
    private final ArrayList<String> betaBranches;

    public BranchController(final UpdaterController updaterController) {
        this.updaterController = updaterController;
        br = updaterController.getBr();
        stableBranches = new ArrayList<String>();
        betaBranches = new ArrayList<String>();
        if (updaterController.getOptions().getBranch() != null) {
            setStaticBranch(updaterController.getOptions().getBranch());
        }

    }

    /**
     * Returns the branch to use for update
     * 
     * @return
     * @throws UpdateException
     * @throws InterruptedException
     */
    public String getCurrentBranch() throws UpdateException, InterruptedException {

        final String branch = getStaticBranch();
        if (branch != null) { return branch; }
        update();
        if (stableBranches.size() == 0) { throw new UpdateException("No BranchList found"); }

        return stableBranches.get(stableBranches.size() - 1);
    }

    public String getLatestStableBranch() throws InterruptedException {
        if (stableBranches.size() == 0) {
            update();
        }
        return stableBranches.get(stableBranches.size() - 1);
    }

    private String getStaticBranch() {

        return JSonStorage.getPlainStorage("WEBUPDATE").get(UpdaterConstants.PARAM_BRANCH, (String) null);
    }

    public void setStaticBranch(final String branch) {

        JSonStorage.getPlainStorage("WEBUPDATE").put(UpdaterConstants.PARAM_BRANCH, branch);
    }

    private void update() throws InterruptedException {

        for (final RandomIterator<String> it = new RandomIterator<String>(UpdaterController.UPDATE_MIRROR); it.hasNext();) {
            final String serv = it.next();
            try {

                br.getPage(serv + "branches.lst");
                if (br.getRequest().getHttpConnection().isOK()) {
                    final String[] bs = org.appwork.utils.Regex.getLines(br.toString());

                    for (final String element : bs) {

                        if (BranchController.isBetaBranch(element)) {
                            betaBranches.add(element);
                        } else {
                            stableBranches.add(element);
                        }

                    }

                    Log.L.fine("Found branches on " + serv + ":\r\n" + br);

                    // final String savedBranch =
                    // JSonWrapper.get("WEBUPDATE").getStringProperty(WebUpdater.PARAM_BRANCH);
                    //
                    // if (branches.length > 0 && savedBranch != null &&
                    // WebUpdater.isBetaBranch(savedBranch)) {
                    //
                    // if (betaBranch == null ||
                    // !savedBranch.equals(betaBranch)) {
                    // JSonWrapper.get("WEBUPDATE").setProperty(WebUpdater.PARAM_BRANCH,
                    // branches[0]);
                    // JSonWrapper.get("WEBUPDATE").save();
                    // JDLogger.getLogger().severe("RESETTED BRANCH; SINCE BETA branch "
                    // + savedBranch + " is not available any more");
                    // }
                    //
                    // }

                    return;
                }
            } catch (final Exception e) {
                e.printStackTrace();
                updaterController.errorWait();
            }
            Log.L.warning("No branches found on " + serv);
        }

    }
}
