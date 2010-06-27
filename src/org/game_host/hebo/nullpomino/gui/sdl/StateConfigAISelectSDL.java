/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
        * Neither the name of NullNoname nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package org.game_host.hebo.nullpomino.gui.sdl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.game_host.hebo.nullpomino.game.subsystem.ai.AIPlayer;
import org.game_host.hebo.nullpomino.util.GeneralUtil;

import sdljava.SDLException;
import sdljava.video.SDLSurface;

/**
 * AI選択画面のステート
 */
public class StateConfigAISelectSDL extends BaseStateSDL {
	/** ログ */
	static Logger log = Logger.getLogger(StateConfigAISelectSDL.class);

	/** 1画面に表示する最大AI数 */
	public static final int MAX_AI_IN_ONE_PAGE = 20;

	/** プレイヤーID */
	public int player = 0;

	/** AIのクラス一覧 */
	protected String[] aiPathList;

	/** AIの名前一覧 */
	protected String[] aiNameList;

	/** 現在のAIのクラス */
	protected String currentAI;

	/** AIのID */
	protected int aiID = 0;

	/** AIの移動間隔 */
	protected int aiMoveDelay = 0;

	/** AIの思考の待ち時間 */
	protected int aiThinkDelay = 0;

	/** AIでスレッドを使う */
	protected boolean aiUseThread = false;

	/** カーソル位置 */
	protected int cursor = 0;

	/**
	 * コンストラクタ
	 */
	public StateConfigAISelectSDL() {
		try {
			BufferedReader in = new BufferedReader(new FileReader("config/list/ai.lst"));
			aiPathList = loadAIList(in);
			aiNameList = loadAINames(aiPathList);
			in.close();
		} catch (IOException e) {
			log.warn("Failed to load AI list", e);
		}
	}

	/*
	 * このステートに入ったときの処理
	 */
	@Override
	public void enter() throws SDLException {
		currentAI = NullpoMinoSDL.propGlobal.getProperty(player + ".ai", "");
		aiMoveDelay = NullpoMinoSDL.propGlobal.getProperty(player + ".aiMoveDelay", 0);
		aiThinkDelay = NullpoMinoSDL.propGlobal.getProperty(player + ".aiThinkDelay", 0);
		aiUseThread = NullpoMinoSDL.propGlobal.getProperty(player + ".aiUseThread", true);

		aiID = -1;
		for(int i = 0; i < aiPathList.length; i++) {
			if(currentAI.equals(aiPathList[i])) aiID = i;
		}
	}

	/**
	 * AI一覧を読み込み
	 * @param bf 読み込み元のテキストファイル
	 * @return AI一覧
	 */
	public String[] loadAIList(BufferedReader bf) {
		ArrayList<String> aiArrayList = new ArrayList<String>();

		while(true) {
			String name = null;
			try {
				name = bf.readLine();
			} catch (Exception e) {
				break;
			}
			if(name == null) break;
			if(name.length() == 0) break;

			aiArrayList.add(name);
		}

		String[] aiStringList = new String[aiArrayList.size()];
		for(int i = 0; i < aiArrayList.size(); i++) aiStringList[i] = aiArrayList.get(i);

		return aiStringList;
	}

	/**
	 * AIの名前一覧を作成
	 * @param aiPath AIのクラスのリスト
	 * @return AIの名前一覧
	 */
	public String[] loadAINames(String[] aiPath) {
		String[] aiName = new String[aiPath.length];

		for(int i = 0; i < aiPath.length; i++) {
			Class<AIPlayer> aiClass;
			AIPlayer aiObj;
			aiName[i] = "(INVALID)";

			try {
				aiClass = (Class<AIPlayer>) Class.forName(aiPath[i]);
				aiObj = aiClass.newInstance();
				aiName[i] = aiObj.getName();
			} catch(ClassNotFoundException e) {
				log.warn("AI class " + aiPath[i] + " not found", e);
			} catch(Throwable e) {
				log.warn("AI class " + aiPath[i] + " load failed", e);
			}
		}

		return aiName;
	}

	/*
	 * 画面描画
	 */
	@Override
	public void render(SDLSurface screen) throws SDLException {
		ResourceHolderSDL.imgMenu.blitSurface(screen);

		NormalFontSDL.printFontGrid(1, 1, (player + 1) + "P AI SETTING", NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		String aiName = "";
		if(aiID < 0) aiName = "(DISABLE)";
		else aiName = aiNameList[aiID].toUpperCase();
		NormalFontSDL.printFontGrid(2, 3, "AI TYPE:" + aiName, (cursor == 0));
		NormalFontSDL.printFontGrid(2, 4, "AI MOVE DELAY:" + aiMoveDelay, (cursor == 1));
		NormalFontSDL.printFontGrid(2, 5, "AI THINK DELAY:" + aiThinkDelay, (cursor == 2));
		NormalFontSDL.printFontGrid(2, 6, "AI USE THREAD:" + GeneralUtil.getONorOFF(aiUseThread), (cursor == 3));

		NormalFontSDL.printFontGrid(1, 28, "A:OK B:CANCEL", NormalFontSDL.COLOR_GREEN);
	}

	/*
	 * 内部状態の更新
	 */
	@Override
	public void update() throws SDLException {
		// カーソル移動
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_UP)) {
			cursor--;
			if(cursor < 0) cursor = 3;
			ResourceHolderSDL.soundManager.play("cursor");
		}
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_DOWN)) {
			cursor++;
			if(cursor > 3) cursor = 0;
			ResourceHolderSDL.soundManager.play("cursor");
		}

		// 設定変更
		int change = 0;
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_LEFT)) change = -1;
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_RIGHT)) change = 1;

		if(change != 0) {
			ResourceHolderSDL.soundManager.play("change");

			switch(cursor) {
			case 0:
				aiID += change;
				if(aiID < -1) aiID = aiNameList.length - 1;
				if(aiID > aiNameList.length - 1) aiID = -1;
				break;
			case 1:
				aiMoveDelay += change;
				if(aiMoveDelay < -1) aiMoveDelay = 99;
				if(aiMoveDelay > 99) aiMoveDelay = -1;
				break;
			case 2:
				aiThinkDelay += change * 10;
				if(aiThinkDelay < 0) aiThinkDelay = 1000;
				if(aiThinkDelay > 1000) aiThinkDelay = 0;
				break;
			case 3:
				aiUseThread = !aiUseThread;
				break;
			}
		}

		// 決定ボタン
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_A)) {
			ResourceHolderSDL.soundManager.play("decide");

			if(aiID >= 0) NullpoMinoSDL.propGlobal.setProperty(player + ".ai", aiPathList[aiID]);
			else NullpoMinoSDL.propGlobal.setProperty(player + ".ai", "");
			NullpoMinoSDL.propGlobal.setProperty(player + ".aiMoveDelay", aiMoveDelay);
			NullpoMinoSDL.propGlobal.setProperty(player + ".aiThinkDelay", aiThinkDelay);
			NullpoMinoSDL.propGlobal.setProperty(player + ".aiUseThread", aiUseThread);
			NullpoMinoSDL.saveConfig();

			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
			return;
		}

		// キャンセルボタン
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_B)) {
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
			return;
		}
	}
}