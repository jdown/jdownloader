package org.jdownloader.update.translate;

import java.io.IOException;
import java.net.URISyntaxException;

import org.appwork.txtresource.TranslationFactory;
import org.appwork.txtresource.TranslationUtils;
import org.appwork.utils.locale.AWUTranslation;

public class T {
    public static final Translation _ = TranslationFactory.create(Translation.class);

    @SuppressWarnings("unchecked")
    public static void main(final String[] args) throws URISyntaxException, IOException {
        TranslationUtils.createFiles(false, Translation.class, AWUTranslation.class);
    }
}
