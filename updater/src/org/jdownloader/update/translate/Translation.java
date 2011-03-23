package org.jdownloader.update.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en", "de" })
public interface Translation extends TranslateInterface {
    //

    @Default(lngs = { "en", "de" }, values = { "Updater outdated. Could not update the updater!", "Updater veraltet. Konnte Updater nicht aktualisieren." })
    String could_not_update_updater();

    @Default(lngs = { "en", "de" }, values = { "Really cancel update?", "Update wirklich abbrechen?" })
    String dialog_rly_cancel();

    @Default(lngs = { "en", "de" }, values = { "The '%s1'-Edition cannot be updated right now. Please try again later!", "Die '%s1' Edition kann momentan nicht aktualisiert werden. Bitte später versuchen." })
    String error_invalid_branch(String name);

    @Default(lngs = { "en", "de" }, values = { "Service not available. Try again in 60 sec.)", "Service momentan nicht erreichbar. Bitte in einer Minute erneut versuchen." })
    String error_service_not_available_right_now();

    @Default(lngs = { "en", "de" }, values = { "Cannot update %s1. \r\n                  Try again later!", "Kann %s1 nicht aktualisieren. \r\n                  Bitte später erneut versuchen." })
    String error_unknown_app(String appID);

    @Default(lngs = { "en", "de" }, values = { "Unknown Edition: %s1", "Unbekannte Version: %s1" })
    String error_unknown_branch(String string);

    @Default(lngs = { "en", "de" }, values = { "Unknown Updateserver problems.\r\nPlease try again later", "Unbekannte Updateprobleme.\r\n                  Bitte versuchen Sie es später erneut." })
    String error_unknown_server();

    @Default(lngs = { "en", "de" }, values = { "Unexpected Error occured. Contact Support.", "Unerwarteter Fehler aufgetreten. Bitte Support kontaktieren." })
    String exception_msg();

    @Default(lngs = { "en", "de" }, values = { "Error occured", "Fehler aufgetreten" })
    String exception_title();

    @Default(lngs = { "en", "de" }, values = { "Close", "Schließen" })
    String exit();

    @Default(lngs = { "en", "de" }, values = { "Update %s1 - %s2 Edition", "Aktualisiere %s1 - %s2 Edition" })
    String guiless_branch_updated(String app, String branch);

    @Default(lngs = { "en", "de" }, values = { "Please wait... preparing updatepackage", "Bitte warten... Bereite Updatepaket vor." })
    String guiless_create_package();

    @Default(lngs = { "en", "de" }, values = { "Update finished.", "Update fertig." })
    String guiless_done();

    @Default(lngs = { "en", "de" }, values = { "Downloading Updatepackage...", "Updatepaket wird heruntergeladen" })
    String guiless_download_data();

    @Default(lngs = { "en", "de" }, values = { "Project contains %s1 file(s)", "Projekt besteht aus %s1 Datei(en)" })
    String guiless_downloaded_hashlist(int size);

    @Default(lngs = { "en", "de" }, values = { "Download finished.", "Download fertiggestellt." })
    String guiless_downloadfinished();

    @Default(lngs = { "en", "de" }, values = { "Error occured. Reverting changes... ", "Fehler aufgetreten. Setze bisherige Änderungen zurück." })
    String guiless_error_occured_start_reverting();

    @Default(lngs = { "en", "de" }, values = { "Extract file(s)...", "Entpacke Datei(en)..." })
    String guiless_extract();

    @Default(lngs = { "en", "de" }, values = { "%s1 file(s) are waiting to get installed!", "%s1 Datei(en) warten auf Installation." })
    String guiless_files_ready_for_install(int size);

    @Default(lngs = { "en", "de" }, values = { "%s1 file(s) are outdated and will be removed.", "%s1 Datei(en) sind verwaltet und werden entfernt." })
    String guiless_files_wait_for_removal(int size);

    @Default(lngs = { "en", "de" }, values = { "Compare filelist with installed file(s)", "Vergleiche Dateiliste mit installierten Datei(en)" })
    String guiless_filtering();

    @Default(lngs = { "en", "de" }, values = { "Installation finished", "Installation abgeschlossen" })
    String guiless_installFinished();

    @Default(lngs = { "en", "de" }, values = { "Cannot access %s2.\r\nPlease close %s1 to continue.", "Kann nicht auf %s2 zugreifen.\r\nBitte beenden Sie %s1 um die Installation zu beenden." })
    String guiless_locked_close_app(String appID, String path);

    @Default(lngs = { "en", "de" }, values = { "%s1 has been closed. Continue now.", "%s1 wurde beendet. Installation wird fortgesetzt." })
    String guiless_locked_closed_app(String appID);

    @Default(lngs = { "en", "de" }, values = { "Bypass Updates. -noupdates flag is set", "Bypass Updates -noupdates Flag ist gesetzt" })
    String guiless_noupdates();

    @Default(lngs = { "en", "de" }, values = { "...Progress: %s1%", "...Fortschritt: %s1%" })
    String guiless_progress(int percent);

    @Default(lngs = { "en", "de" }, values = { "Progress: %s1% | %s2/%s3", "Fortschritt: %s1% | %s2/%s3" })
    void guiless_progress(long l, String loaded, String total);

    @Default(lngs = { "en", "de" }, values = { "Updater will update itself... ", "Updater wird sich nun selbst aktualisieren..." })
    String guiless_selfupdate();

    @Default(lngs = { "en", "de" }, values = { "Start installing %s1 file(s)", "Starte Installation von %s1 Datei(en)" })
    String guiless_start_install(int size);

    @Default(lngs = { "en", "de" }, values = { "Start Download: %s1", "Beginne Download: %s1" })
    String guiless_started_download(String formatBytes);

    @Default(lngs = { "en", "de" }, values = { "Update failed. Cause: \r\n%s1", "Update fehlgeschlagen: Grund: \r\n%s1" })
    String guiless_update_failed(String message);

    @Default(lngs = { "en", "de" }, values = { "Update failed. Unknown Reason.", "Update fehlgeschlagen: Unbekannter Grund" })
    String guiless_update_failed_unknown();

    @Default(lngs = { "en", "de" }, values = { "%s1 file(s) must be downloaded", "%s1 Datei(en) müssen heruntergeladen werden." })
    String guiless_updates_waiting_for_download(int size);

    @Default(lngs = { "en", "de" }, values = { "User interrupted updated", "Benutzer hat Update unterbrochen." })
    String guiless_userinterrupted();

    @Default(lngs = { "en", "de" }, values = { "You already have the latest version", "Sie nutzen bereits die aktuelle Version." })
    String guiless_you_are_up2date();

    @Default(lngs = { "en", "de" }, values = { "Ask me later!", "Später nachfragen!" })
    String install_updates_later();

    @Default(lngs = { "en", "de" }, values = { "Install now!", "Jetzt installieren!" })
    String install_updates_now();

    @Default(lngs = { "en", "de" }, values = { "Update has to restart now.", "Updater muss jetzt neu starten." })
    String restart_msg();

    @Default(lngs = { "en", "de" }, values = { "Updater will restart itself now", "Updater wird sich nun selbst neu starten" })
    String restart_required_msg();

    @Default(lngs = { "en", "de" }, values = { "Restart Required", "Updater Neustart benötigt" })
    String restart_required_title();

    @Default(lngs = { "en", "de" }, values = { "Restart!", "Neustart" })
    String restart_title();

    @Default(lngs = { "en", "de" }, values = { "Started Updater", "Updater gestartet" })
    String start();

    @Default(lngs = { "en", "de" }, values = { "Update(s) found!", "Update(s) gefunden!" })
    String udpates_found();

    @Default(lngs = { "en", "de" }, values = { "Cancel", "Abbrechen" })
    String update_dialog_cancel();

    @Default(lngs = { "en", "de" }, values = { "Ask me later", "Später erneut fragen" })
    String update_dialog_later();

    @Default(lngs = { "en", "de" }, values = { "Updates are ready for Installation. Do you want to run the update now?\r\n\r\nDetails:\r\n    * Install %s1 new file(s)\r\n    * Remove %s3 outdated file(s)", "Updates können jetzt installiert werden. Soll das Update jetzt gestartet werden?\r\n\r\nDetails:\r\n    * %s1 neue Datei(en) installieren\r\n    * %s2 veraltete Datei(en) entfernen" })
    String update_dialog_msg_x_updates_available(int install, int remove);

    @Default(lngs = { "en", "de" }, values = { "Open Changelog", "Änderungen ansehen" })
    String update_dialog_news_button();

    @Default(lngs = { "en", "de" }, values = { "http://www.jdownloader.org/changelog", "http://www.jdownloader.org/changelog" })
    String update_dialog_news_button_url();

    @Default(lngs = { "en", "de" }, values = { "Update(s) available", "Update(s) verfügbar" })
    String update_dialog_title_updates_available();

    @Default(lngs = { "en", "de" }, values = { "Yes(recommended)", "Ja(empfohlen)" })
    String update_dialog_yes();

    @Default(lngs = { "en", "de" }, values = { "No Internet connection to updateserver: %s1", "Keine Internetverbindung zum Updateserver: %s1" })
    String UpdateException_socket(String message);

    @Default(lngs = { "en", "de" }, values = { "Updater failed to update himself. Please try again in a few minutes.", "Updater konnte sich nicht selbst aktualisieren. Bitte versuch es in einigen Minuten nochmal." })
    String updateloop();

    @Default(lngs = { "en", "de" }, values = { "Updateloop detected!", "Updateschleife endeckt!" })
    String updateloop_title();

    @Default(lngs = { "en", "de" }, values = { "%s1 update(s) are ready for installation.", "%s1 Update(s) können jetzt installiert werden." })
    String updates_are_ready_for_install_now(int size);

    @Default(lngs = { "en", "de" }, values = { "%s1 update(s) are ready for installation.", "%s1 Update(s) können jetzt installiert werden." })
    String updates_ready_for_install(int size);

}
