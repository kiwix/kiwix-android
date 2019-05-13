package org.kiwix.kiwixmobile.database;

import com.yahoo.squidb.data.SimpleDataChangedNotifier;
import com.yahoo.squidb.sql.Table;
import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.TimeUnit;
import kotlin.Unit;

/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
abstract class BaseDao {
  private final PublishProcessor<Unit> updates = PublishProcessor.create();
  protected final KiwixDatabase kiwixDatabase;

  public BaseDao(KiwixDatabase kiwixDatabase, Table table) {
    this.kiwixDatabase = kiwixDatabase;
    kiwixDatabase.registerDataChangedNotifier(
        new SimpleDataChangedNotifier(table) {
          @Override
          protected void onDataChanged() {
            updates.onNext(Unit.INSTANCE);
          }
        });
    updates
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(unit -> {
              onUpdateToTable();
            }
            , Throwable::printStackTrace
        );
    Flowable.timer(100, TimeUnit.MILLISECONDS).subscribe(aLong -> updates.onNext(Unit.INSTANCE));
  }

  protected abstract void onUpdateToTable();
}
