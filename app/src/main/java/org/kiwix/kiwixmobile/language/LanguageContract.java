package org.kiwix.kiwixmobile.language;

import org.kiwix.kiwixmobile.base.BaseContract;
import org.kiwix.kiwixmobile.models.Language;

import java.util.List;

interface LanguageContract {
  interface View extends BaseContract.View<Presenter> {
    void notifyLanguagesFiltered(List<Language> languages);
  }

  interface Presenter extends BaseContract.Presenter<View> {
    void filerLanguages(List<Language> languages, String query);

    void saveLanguages(List<Language> languages);
  }
}
