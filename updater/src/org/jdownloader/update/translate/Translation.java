package org.jdownloader.update.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en", "de" })
public interface Translation extends TranslateInterface {
    //

    @Default(lngs = { "en", "de" }, values = { "Really cancel update?", "Update wirklich abbrechen?" })
    String dialog_rly_cancel();

    @Default(lngs = { "en", "de" }, values = { "Close", "Schließen" })
    String exit();

    @Default(lngs = { "en", "de" }, values = { "Ask me later!", "Später nachfragen!" })
    String install_updates_later();

    @Default(lngs = { "en", "de" }, values = { "Install now!", "Jetzt installieren!" })
    String install_updates_now();

    @Default(lngs = { "en", "de" }, values = { "Update(s) found!", "Update(s) gefunden!" })
    String udpates_found();

    @Default(lngs = { "en", "de" }, values = { "Cancel", "Abbrechen" })
    String update_dialog_cancel();

    @Default(lngs = { "en", "de" }, values = { "Ask me later", "Später erneut fragen" })
    String update_dialog_later();

    @Default(lngs = { "en", "de" }, values = { "%s1 update(s) are ready for Installation. Do you want to run the update now?", "%s1 Update(s) können jetzt installiert werden. Wollen Sie jetzt aktualisieren? " })
    String update_dialog_msg_x_updates_available(int num);

    @Default(lngs = { "en", "de" }, values = { "Open Changelog", "Änderungen ansehen" })
    String update_dialog_news_button();

    @Default(lngs = { "en", "de" }, values = { "http://www.jdownloader.org/changelog", "http://www.jdownloader.org/changelog" })
    String update_dialog_news_button_url();

    @Default(lngs = { "en", "de" }, values = { "Update(s) available", "Update(s) verfügbar" })
    String update_dialog_title_updates_available();

    @Default(lngs = { "en", "de" }, values = { "Yes(recommended)", "Ja(empfohlen)" })
    String update_dialog_yes();

    @Default(lngs = { "en", "de" }, values = { "%s1 update(s) are ready for installation.", "%s1 Update(s) können jetzt installiert werden." })
    String updates_are_ready_for_install_now(int size);

    @Default(lngs = { "en", "de" }, values = { "%s1 update(s) are ready for installation.", "%s1 Update(s) können jetzt installiert werden." })
    String updates_ready_for_install(int size);

}
