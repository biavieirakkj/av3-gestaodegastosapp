package com.example.controlefinanceiro.data.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class Migracoes {

    public static final Migration MIGRATION_1_2 =
            new Migration(1, 2) {

                @Override
                public void migrate(
                        @NonNull SupportSQLiteDatabase database
                ) {

                    database.execSQL(
                            "ALTER TABLE lembretes " +
                                    "ADD COLUMN mensal INTEGER NOT NULL DEFAULT 0"
                    );

                    database.execSQL(
                            "ALTER TABLE lembretes " +
                                    "ADD COLUMN diaVencimento INTEGER NOT NULL DEFAULT 0"
                    );

                    database.execSQL(
                            "ALTER TABLE lembretes " +
                                    "ADD COLUMN diasAntes INTEGER NOT NULL DEFAULT 0"
                    );
                }
            };
}
