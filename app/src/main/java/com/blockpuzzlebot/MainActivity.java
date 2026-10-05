package com.blockpuzzlebot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(40,60,40,40);
        TextView title = new TextView(this); title.setText("Block Puzzle Bot\n\n1. Разреши службу доступности.\n2. Вернись в игру.\n3. Нажми «Запустить».\n\nБот анализирует только экран и делает жесты."); title.setTextSize(18);
        Button settings = new Button(this); settings.setText("Открыть специальные возможности"); settings.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        Button start = new Button(this); start.setText("Запустить бота"); start.setOnClickListener(v -> BotAccessibilityService.running = true);
        Button stop = new Button(this); stop.setText("Остановить бота"); stop.setOnClickListener(v -> BotAccessibilityService.running = false);
        root.addView(title); root.addView(settings); root.addView(start); root.addView(stop); setContentView(root);
    }
}
