//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.captcha;

import java.io.File;

import jd.captcha.utils.UTILITIES;
import jd.utils.JDUtilities;

/**
 * JAC Tester
 * 
 * 
 * @author JD-Team
 */
public class JACTest {
    /**
     * @param args
     */
    public static void main(String args[]) {

        JACTest main = new JACTest();
        main.go();
    }

    private void go() {
        String methodsPath = UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(), "jd", "captcha", "methods" });
        String hoster = "ff";

        JAntiCaptcha jac = new JAntiCaptcha(methodsPath, hoster);

        // jac.setShowDebugGui(true);
        // LetterComperator.CREATEINTERSECTIONLETTER = true;
        //
        // jac.exportDB();
        // UTILITIES.getLogger().info("has method:
        // "+JAntiCaptcha.hasMethod(methodsPath, hoster));

        //

        // jac.importDB();

        //jac.displayLibrary();

        // jac.getJas().set("preScanFilter", 0);
        // jac.trainCaptcha(new
        // File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath()+"/jd/captcha/methods"+"/"+hoster+"/captchas/"+"securedin1730080724541.jpg"),
        // 4);

        jac.showPreparedCaptcha(new File("C:/Users/coalado/.jd_home/jd/captcha/methods/" + hoster + "/captchas/" + "captcha_ff_codef1p1h1s" + ".png"));

        // UTILITIES.getLogger().info(JAntiCaptcha.getCaptchaCode(UTILITIES.loadImage(new
        // File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath()+"/jd/captcha/methods"+"/rapidshare.com/captchas/rapidsharecom24190807214810.jpg")),
        // null, "rapidshare.com"));
        // jac.removeBadLetters();
        // jac.addLetterMap();
        // jac.saveMTHFile();

    }
}