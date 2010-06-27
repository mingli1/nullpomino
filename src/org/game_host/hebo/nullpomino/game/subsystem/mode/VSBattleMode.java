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
package org.game_host.hebo.nullpomino.game.subsystem.mode;

import java.util.LinkedList;
import java.util.Random;

import org.game_host.hebo.nullpomino.game.component.BGMStatus;
import org.game_host.hebo.nullpomino.game.component.Block;
import org.game_host.hebo.nullpomino.game.component.Controller;
import org.game_host.hebo.nullpomino.game.component.Field;
import org.game_host.hebo.nullpomino.game.component.Piece;
import org.game_host.hebo.nullpomino.game.event.EventReceiver;
import org.game_host.hebo.nullpomino.game.play.GameEngine;
import org.game_host.hebo.nullpomino.game.play.GameManager;
import org.game_host.hebo.nullpomino.util.CustomProperties;
import org.game_host.hebo.nullpomino.util.GeneralUtil;

/**
 * VS-BATTLEモード
 */
public class VSBattleMode extends DummyMode {
	/** 現在のバージョン */
	private static final int CURRENT_VERSION = 4;

	/** プレイヤーの数 */
	private static final int MAX_PLAYERS = 2;

	/** 直前のスコア獲得の種類の定数 */
	private static final int EVENT_NONE = 0,
							 EVENT_SINGLE = 1,
							 EVENT_DOUBLE = 2,
							 EVENT_TRIPLE = 3,
							 EVENT_FOUR = 4,
							 EVENT_TSPIN_SINGLE_MINI = 5,
							 EVENT_TSPIN_SINGLE = 6,
							 EVENT_TSPIN_DOUBLE = 7,
							 EVENT_TSPIN_TRIPLE = 8,
							 EVENT_TSPIN_DOUBLE_MINI = 9;

	/** コンボの攻撃力 */
	private final int[] COMBO_ATTACK_TABLE = {0,0,1,1,2,2,3,3,4,4,4,5};

	/** 邪魔ブロックの穴の位置が普通にランダムに変わる */
	private final int GARBAGE_TYPE_NORMAL = 0;

	/** 邪魔ブロックの穴の位置が1回のせり上がりで変わらない */
	private final int GARBAGE_TYPE_NOCHANGE_ONE_RISE = 1;

	/** 邪魔ブロックの穴の位置が1回の攻撃で変わらない(2回以上なら変わる) */
	private final int GARBAGE_TYPE_NOCHANGE_ONE_ATTACK = 2;

	/** 邪魔ブロックタイプの表示名 */
	private final String[] GARBAGE_TYPE_STRING = {"NORMAL", "ONE RISE", "1-ATTACK"};

	/** 各プレイヤーの邪魔ブロックの色 */
	private final int[] PLAYER_COLOR_BLOCK = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};

	/** 各プレイヤーの枠の色 */
	private final int[] PLAYER_COLOR_FRAME = {GameEngine.FRAME_COLOR_RED, GameEngine.FRAME_COLOR_BLUE};

	/** このモードを所有するGameManager */
	private GameManager owner;

	/** 描画などのイベント処理 */
	private EventReceiver receiver;

	/** 邪魔ブロックのタイプ */
	private int[] garbageType;

	/** 溜まっている邪魔ブロックの数 */
	private int[] garbage;

	/** 送った邪魔ブロックの数 */
	private int[] garbageSent;

	/** 最後にスコア獲得してから経過した時間 */
	private int[] scgettime;

	/** 直前のスコア獲得の種類 */
	private int[] lastevent;

	/** 直前のスコア獲得でB2Bだったらtrue */
	private boolean[] lastb2b;

	/** 直前のスコア獲得でのコンボ数 */
	private int[] lastcombo;

	/** 直前のスコア獲得でのピースID */
	private int[] lastpiece;

	/** 使用するBGM */
	private int bgmno;

	/** T-Spin有効フラグ(0=なし 1=普通 2=全スピン) */
	private int[] tspinEnableType;

	/** 旧T-Spin有効フラグ */
	private boolean[] enableTSpin;

	/** 壁蹴りありT-Spin有効 */
	private boolean[] enableTSpinKick;

	/** B2B有効 */
	private boolean[] enableB2B;

	/** コンボ有効 */
	private boolean[] enableCombo;

	/** ビッグ */
	private boolean[] big;

	/** 効果音ON/OFF */
	private boolean[] enableSE;

	/** Hurryup開始までの秒数(-1でHurryupなし) */
	private int[] hurryupSeconds;

	/** Hurryup後に何回ブロックを置くたびに床をせり上げるか */
	private int[] hurryupInterval;

	/** マップ使用フラグ */
	private boolean[] useMap;

	/** 使用するマップセット番号 */
	private int[] mapSet;

	/** マップ番号(-1でランダム) */
	private int[] mapNumber;

	/** 最後に使ったプリセット番号 */
	private int[] presetNumber;

	/** 勝者 */
	private int winnerID;

	/** 敵から送られてきた邪魔ブロックのリスト */
	private LinkedList<GarbageEntry>[] garbageEntries;

	/** Hurryup後にブロックを置いた回数 */
	private int[] hurryupCount;

	/** マップセットのプロパティファイル */
	private CustomProperties[] propMap;

	/** 最大マップ番号 */
	private int[] mapMaxNo;

	/** バックアップ用フィールド（マップをリプレイに保存するときに使用） */
	private Field[] fldBackup;

	/** マップ選択用乱数 */
	private Random randMap;

	/** バージョン */
	private int version;

	/*
	 * モード名
	 */
	@Override
	public String getName() {
		return "VS-BATTLE";
	}

	/*
	 * プレイヤー数
	 */
	@Override
	public int getPlayers() {
		return MAX_PLAYERS;
	}

	/*
	 * モードの初期化
	 */
	@Override
	public void modeInit(GameManager manager) {
		owner = manager;
		receiver = owner.receiver;

		garbageType = new int[MAX_PLAYERS];
		garbage = new int[MAX_PLAYERS];
		garbageSent = new int[MAX_PLAYERS];

		scgettime = new int[MAX_PLAYERS];
		lastevent = new int[MAX_PLAYERS];
		lastb2b = new boolean[MAX_PLAYERS];
		lastcombo = new int[MAX_PLAYERS];
		lastpiece = new int[MAX_PLAYERS];
		bgmno = 0;
		tspinEnableType = new int[MAX_PLAYERS];
		enableTSpin = new boolean[MAX_PLAYERS];
		enableTSpinKick = new boolean[MAX_PLAYERS];
		enableB2B = new boolean[MAX_PLAYERS];
		enableCombo = new boolean[MAX_PLAYERS];
		big = new boolean[MAX_PLAYERS];
		enableSE = new boolean[MAX_PLAYERS];
		hurryupSeconds = new int[MAX_PLAYERS];
		hurryupInterval = new int[MAX_PLAYERS];
		useMap = new boolean[MAX_PLAYERS];
		mapSet = new int[MAX_PLAYERS];
		mapNumber = new int[MAX_PLAYERS];
		presetNumber = new int[MAX_PLAYERS];
		garbageEntries = new LinkedList[MAX_PLAYERS];
		hurryupCount = new int[MAX_PLAYERS];
		propMap = new CustomProperties[MAX_PLAYERS];
		mapMaxNo = new int[MAX_PLAYERS];
		fldBackup = new Field[MAX_PLAYERS];
		randMap = new Random();

		winnerID = -1;
	}

	/**
	 * スピードプリセットを読み込み
	 * @param engine GameEngine
	 * @param prop 読み込み元のプロパティファイル
	 * @param preset プリセット番号
	 */
	private void loadPreset(GameEngine engine, CustomProperties prop, int preset) {
		engine.speed.gravity = prop.getProperty("vsbattle.gravity." + preset, 4);
		engine.speed.denominator = prop.getProperty("vsbattle.denominator." + preset, 256);
		engine.speed.are = prop.getProperty("vsbattle.are." + preset, 24);
		engine.speed.areLine = prop.getProperty("vsbattle.areLine." + preset, 24);
		engine.speed.lineDelay = prop.getProperty("vsbattle.lineDelay." + preset, 10);
		engine.speed.lockDelay = prop.getProperty("vsbattle.lockDelay." + preset, 30);
		engine.speed.das = prop.getProperty("vsbattle.das." + preset, 14);
	}

	/**
	 * スピードプリセットを保存
	 * @param engine GameEngine
	 * @param prop 保存先のプロパティファイル
	 * @param preset プリセット番号
	 */
	private void savePreset(GameEngine engine, CustomProperties prop, int preset) {
		prop.setProperty("vsbattle.gravity." + preset, engine.speed.gravity);
		prop.setProperty("vsbattle.denominator." + preset, engine.speed.denominator);
		prop.setProperty("vsbattle.are." + preset, engine.speed.are);
		prop.setProperty("vsbattle.areLine." + preset, engine.speed.areLine);
		prop.setProperty("vsbattle.lineDelay." + preset, engine.speed.lineDelay);
		prop.setProperty("vsbattle.lockDelay." + preset, engine.speed.lockDelay);
		prop.setProperty("vsbattle.das." + preset, engine.speed.das);
	}

	/**
	 * スピード以外の設定を読み込み
	 * @param engine GameEngine
	 * @param prop 読み込み元のプロパティファイル
	 */
	private void loadOtherSetting(GameEngine engine, CustomProperties prop) {
		int playerID = engine.playerID;
		bgmno = prop.getProperty("vsbattle.bgmno", 0);
		garbageType[playerID] = prop.getProperty("vsbattle.garbageType", GARBAGE_TYPE_NOCHANGE_ONE_ATTACK);
		tspinEnableType[playerID] = prop.getProperty("vsbattle.tspinEnableType.p" + playerID, 1);
		enableTSpin[playerID] = prop.getProperty("vsbattle.enableTSpin.p" + playerID, true);
		enableTSpinKick[playerID] = prop.getProperty("vsbattle.enableTSpinKick.p" + playerID, true);
		enableB2B[playerID] = prop.getProperty("vsbattle.enableB2B.p" + playerID, true);
		enableCombo[playerID] = prop.getProperty("vsbattle.enableCombo.p" + playerID, true);
		big[playerID] = prop.getProperty("vsbattle.big.p" + playerID, false);
		enableSE[playerID] = prop.getProperty("vsbattle.enableSE.p" + playerID, true);
		hurryupSeconds[playerID] = prop.getProperty("vsbattle.hurryupSeconds.p" + playerID, -1);
		hurryupInterval[playerID] = prop.getProperty("vsbattle.hurryupInterval.p" + playerID, 5);
		useMap[playerID] = prop.getProperty("vsbattle.useMap.p" + playerID, false);
		mapSet[playerID] = prop.getProperty("vsbattle.mapSet.p" + playerID, 0);
		mapNumber[playerID] = prop.getProperty("vsbattle.mapNumber.p" + playerID, -1);
		presetNumber[playerID] = prop.getProperty("vsbattle.presetNumber.p" + playerID, 0);
	}

	/**
	 * スピード以外の設定を保存
	 * @param engine GameEngine
	 * @param prop 保存先のプロパティファイル
	 */
	private void saveOtherSetting(GameEngine engine, CustomProperties prop) {
		int playerID = engine.playerID;
		prop.setProperty("vsbattle.bgmno", bgmno);
		prop.setProperty("vsbattle.garbageType", garbageType[playerID]);
		prop.setProperty("vsbattle.tspinEnableType.p" + playerID, tspinEnableType[playerID]);
		prop.setProperty("vsbattle.enableTSpin.p" + playerID, enableTSpin[playerID]);
		prop.setProperty("vsbattle.enableTSpinKick.p" + playerID, enableTSpinKick[playerID]);
		prop.setProperty("vsbattle.enableB2B.p" + playerID, enableB2B[playerID]);
		prop.setProperty("vsbattle.enableCombo.p" + playerID, enableCombo[playerID]);
		prop.setProperty("vsbattle.big.p" + playerID, big[playerID]);
		prop.setProperty("vsbattle.enableSE.p" + playerID, enableSE[playerID]);
		prop.setProperty("vsbattle.hurryupSeconds.p" + playerID, hurryupSeconds[playerID]);
		prop.setProperty("vsbattle.hurryupInterval.p" + playerID, hurryupInterval[playerID]);
		prop.setProperty("vsbattle.useMap.p" + playerID, useMap[playerID]);
		prop.setProperty("vsbattle.mapSet.p" + playerID, mapSet[playerID]);
		prop.setProperty("vsbattle.mapNumber.p" + playerID, mapNumber[playerID]);
		prop.setProperty("vsbattle.presetNumber.p" + playerID, presetNumber[playerID]);
	}

	/**
	 * マップ読み込み
	 * @param field フィールド
	 * @param prop 読み込み元のプロパティファイル
	 * @param preset 任意のID
	 */
	private void loadMap(Field field, CustomProperties prop, int id) {
		field.reset();
		//field.readProperty(prop, id);
		field.stringToField(prop.getProperty("map." + id, ""));
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true);
		field.setAllAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED, false);
	}

	/**
	 * マップ保存
	 * @param field フィールド
	 * @param prop 保存先のプロパティファイル
	 * @param id 任意のID
	 */
	private void saveMap(Field field, CustomProperties prop, int id) {
		//field.writeProperty(prop, id);
		prop.setProperty("map." + id, field.fieldToString());
	}

	/**
	 * 今溜まっている邪魔ブロックの数を返す
	 * @param playerID プレイヤーID
	 * @return 今溜まっている邪魔ブロックの数
	 */
	private int getTotalGarbageLines(int playerID) {
		int count = 0;
		for(GarbageEntry garbageEntry: garbageEntries[playerID]) {
			count += garbageEntry.lines;
		}
		return count;
	}

	/**
	 * プレビュー用にマップを読み込み
	 * @param engine GameEngine
	 * @param playerID プレイヤー番号
	 * @param id マップID
	 * @param forceReload trueにするとマップファイルを強制再読み込み
	 */
	private void loadMapPreview(GameEngine engine, int playerID, int id, boolean forceReload) {
		if((propMap[playerID] == null) || (forceReload)) {
			mapMaxNo[playerID] = 0;
			propMap[playerID] = receiver.loadProperties("config/map/vsbattle/" + mapSet[playerID] + ".map");
		}

		if((propMap[playerID] == null) && (engine.field != null)) {
			engine.field.reset();
		} else if(propMap[playerID] != null) {
			mapMaxNo[playerID] = propMap[playerID].getProperty("map.maxMapNumber", 0);
			engine.createFieldIfNeeded();
			loadMap(engine.field, propMap[playerID], id);
			engine.field.setAllSkin(engine.getSkin());
		}
	}

	/*
	 * 各プレイヤーの初期化
	 */
	@Override
	public void playerInit(GameEngine engine, int playerID) {
		if(playerID == 1) {
			engine.randSeed = owner.engine[0].randSeed;
			engine.random = new Random(owner.engine[0].randSeed);
		}

		engine.framecolor = PLAYER_COLOR_FRAME[playerID];

		garbage[playerID] = 0;
		garbageSent[playerID] = 0;
		scgettime[playerID] = 0;
		lastevent[playerID] = EVENT_NONE;
		lastb2b[playerID] = false;
		lastcombo[playerID] = 0;

		garbageEntries[playerID] = new LinkedList<GarbageEntry>();

		hurryupCount[playerID] = 0;

		if(engine.owner.replayMode == false) {
			loadOtherSetting(engine, engine.owner.modeConfig);
			loadPreset(engine, engine.owner.modeConfig, -1 - playerID);
			version = CURRENT_VERSION;
		} else {
			loadOtherSetting(engine, engine.owner.replayProp);
			loadPreset(engine, engine.owner.replayProp, -1 - playerID);
			version = owner.replayProp.getProperty("vsbattle.version", 0);
		}
	}

	/*
	 * 設定画面の処理
	 */
	@Override
	public boolean onSetting(GameEngine engine, int playerID) {
		// メニュー
		if((engine.owner.replayMode == false) && (engine.statc[4] == 0)) {
			// 上
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
				engine.statc[2]--;
				if(engine.statc[2] < 0) engine.statc[2] = 21;
				engine.playSE("cursor");
			}
			// 下
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
				engine.statc[2]++;
				if(engine.statc[2] > 21) engine.statc[2] = 0;
				engine.playSE("cursor");
			}

			// 設定変更
			int change = 0;
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)) change = -1;
			if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) change = 1;

			if(change != 0) {
				engine.playSE("change");

				int m = 1;
				if(engine.ctrl.isPress(Controller.BUTTON_E)) m = 100;
				if(engine.ctrl.isPress(Controller.BUTTON_F)) m = 1000;

				switch(engine.statc[2]) {
				case 0:
					engine.speed.gravity += change * m;
					if(engine.speed.gravity < -1) engine.speed.gravity = 99999;
					if(engine.speed.gravity > 99999) engine.speed.gravity = -1;
					break;
				case 1:
					engine.speed.denominator += change * m;
					if(engine.speed.denominator < -1) engine.speed.denominator = 99999;
					if(engine.speed.denominator > 99999) engine.speed.denominator = -1;
					break;
				case 2:
					engine.speed.are += change;
					if(engine.speed.are < 0) engine.speed.are = 99;
					if(engine.speed.are > 99) engine.speed.are = 0;
					break;
				case 3:
					engine.speed.areLine += change;
					if(engine.speed.areLine < 0) engine.speed.areLine = 99;
					if(engine.speed.areLine > 99) engine.speed.areLine = 0;
					break;
				case 4:
					engine.speed.lineDelay += change;
					if(engine.speed.lineDelay < 0) engine.speed.lineDelay = 99;
					if(engine.speed.lineDelay > 99) engine.speed.lineDelay = 0;
					break;
				case 5:
					engine.speed.lockDelay += change;
					if(engine.speed.lockDelay < 0) engine.speed.lockDelay = 99;
					if(engine.speed.lockDelay > 99) engine.speed.lockDelay = 0;
					break;
				case 6:
					engine.speed.das += change;
					if(engine.speed.das < 0) engine.speed.das = 99;
					if(engine.speed.das > 99) engine.speed.das = 0;
					break;
				case 7:
				case 8:
					presetNumber[playerID] += change;
					if(presetNumber[playerID] < 0) presetNumber[playerID] = 99;
					if(presetNumber[playerID] > 99) presetNumber[playerID] = 0;
					break;
				case 9:
					garbageType[playerID] += change;
					if(garbageType[playerID] < 0) garbageType[playerID] = 2;
					if(garbageType[playerID] > 2) garbageType[playerID] = 0;
					break;
				case 10:
					//enableTSpin[playerID] = !enableTSpin[playerID];
					tspinEnableType[playerID] += change;
					if(tspinEnableType[playerID] < 0) tspinEnableType[playerID] = 2;
					if(tspinEnableType[playerID] > 2) tspinEnableType[playerID] = 0;
					break;
				case 11:
					enableTSpinKick[playerID] = !enableTSpinKick[playerID];
					break;
				case 12:
					enableB2B[playerID] = !enableB2B[playerID];
					break;
				case 13:
					enableCombo[playerID] = !enableCombo[playerID];
					break;
				case 14:
					big[playerID] = !big[playerID];
					break;
				case 15:
					enableSE[playerID] = !enableSE[playerID];
					break;
				case 16:
					hurryupSeconds[playerID] += change;
					if(hurryupSeconds[playerID] < -1) hurryupSeconds[playerID] = 300;
					if(hurryupSeconds[playerID] > 300) hurryupSeconds[playerID] = -1;
					break;
				case 17:
					hurryupInterval[playerID] += change;
					if(hurryupInterval[playerID] < 1) hurryupInterval[playerID] = 99;
					if(hurryupInterval[playerID] > 99) hurryupInterval[playerID] = 1;
					break;
				case 18:
					bgmno += change;
					if(bgmno < 0) bgmno = BGMStatus.BGM_COUNT - 1;
					if(bgmno > BGMStatus.BGM_COUNT - 1) bgmno = 0;
					break;
				case 19:
					useMap[playerID] = !useMap[playerID];
					if(!useMap[playerID]) {
						if(engine.field != null) engine.field.reset();
					} else {
						loadMapPreview(engine, playerID, (mapNumber[playerID] < 0) ? 0 : mapNumber[playerID], true);
					}
					break;
				case 20:
					mapSet[playerID] += change;
					if(mapSet[playerID] < 0) mapSet[playerID] = 99;
					if(mapSet[playerID] > 99) mapSet[playerID] = 0;
					if(useMap[playerID]) {
						mapNumber[playerID] = -1;
						loadMapPreview(engine, playerID, (mapNumber[playerID] < 0) ? 0 : mapNumber[playerID], true);
					}
					break;
				case 21:
					if(useMap[playerID]) {
						mapNumber[playerID] += change;
						if(mapNumber[playerID] < -1) mapNumber[playerID] = mapMaxNo[playerID] - 1;
						if(mapNumber[playerID] > mapMaxNo[playerID] - 1) mapNumber[playerID] = -1;
						loadMapPreview(engine, playerID, (mapNumber[playerID] < 0) ? 0 : mapNumber[playerID], true);
					} else {
						mapNumber[playerID] = -1;
					}
					break;
				}
			}

			// 決定
			if(engine.ctrl.isPush(Controller.BUTTON_A) && (engine.statc[3] >= 5)) {
				engine.playSE("decide");

				if(engine.statc[2] == 7) {
					loadPreset(engine, owner.modeConfig, presetNumber[playerID]);
				} else if(engine.statc[2] == 8) {
					savePreset(engine, owner.modeConfig, presetNumber[playerID]);
					receiver.saveModeConfig(owner.modeConfig);
				} else {
					saveOtherSetting(engine, owner.modeConfig);
					savePreset(engine, owner.modeConfig, -1 - playerID);
					receiver.saveModeConfig(owner.modeConfig);
					engine.statc[4] = 1;
				}
			}

			// キャンセル
			if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				engine.quitflag = true;
			}

			// プレビュー用マップ読み込み
			if(useMap[playerID] && (engine.statc[3] == 0)) {
				loadMapPreview(engine, playerID, (mapNumber[playerID] < 0) ? 0 : mapNumber[playerID], true);
			}

			// ランダムマッププレビュー
			if(useMap[playerID] && (propMap[playerID] != null) && (mapNumber[playerID] < 0)) {
				if(engine.statc[3] % 30 == 0) {
					engine.statc[5]++;
					if(engine.statc[5] >= mapMaxNo[playerID]) engine.statc[5] = 0;
					loadMapPreview(engine, playerID, engine.statc[5], false);
				}
			}

			engine.statc[3]++;
		} else if(engine.statc[4] == 0) {
			engine.statc[3]++;
			engine.statc[2] = 0;

			if(engine.statc[3] >= 60) {
				engine.statc[2] = 9;
			}
			if(engine.statc[3] >= 120) {
				engine.statc[4] = 1;
			}
		} else {
			// 開始
			if((owner.engine[0].statc[4] == 1) && (owner.engine[1].statc[4] == 1) && (playerID == 1)) {
				owner.engine[0].stat = GameEngine.STAT_READY;
				owner.engine[1].stat = GameEngine.STAT_READY;
				owner.engine[0].resetStatc();
				owner.engine[1].resetStatc();
			}
			// キャンセル
			else if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				engine.statc[4] = 0;
			}
		}

		return true;
	}

	/*
	 * 設定画面の描画
	 */
	@Override
	public void renderSetting(GameEngine engine, int playerID) {
		if(engine.statc[4] == 0) {
			if(engine.statc[2] < 9) {
				if(owner.replayMode == false) {
					receiver.drawMenuFont(engine, playerID, 0, (engine.statc[2] * 2) + 1, "b",
										  (playerID == 0) ? EventReceiver.COLOR_RED : EventReceiver.COLOR_BLUE);
				}

				receiver.drawMenuFont(engine, playerID, 0,  0, "GRAVITY", EventReceiver.COLOR_ORANGE);
				receiver.drawMenuFont(engine, playerID, 1,  1, String.valueOf(engine.speed.gravity), (engine.statc[2] == 0));
				receiver.drawMenuFont(engine, playerID, 0,  2, "G-MAX", EventReceiver.COLOR_ORANGE);
				receiver.drawMenuFont(engine, playerID, 1,  3, String.valueOf(engine.speed.denominator), (engine.statc[2] == 1));
				receiver.drawMenuFont(engine, playerID, 0,  4, "ARE", EventReceiver.COLOR_ORANGE);
				receiver.drawMenuFont(engine, playerID, 1,  5, String.valueOf(engine.speed.are), (engine.statc[2] == 2));
				receiver.drawMenuFont(engine, playerID, 0,  6, "ARE LINE", EventReceiver.COLOR_ORANGE);
				receiver.drawMenuFont(engine, playerID, 1,  7, String.valueOf(engine.speed.areLine), (engine.statc[2] == 3));
				receiver.drawMenuFont(engine, playerID, 0,  8, "LINE DELAY", EventReceiver.COLOR_ORANGE);
				receiver.drawMenuFont(engine, playerID, 1,  9, String.valueOf(engine.speed.lineDelay), (engine.statc[2] == 4));
				receiver.drawMenuFont(engine, playerID, 0, 10, "LOCK DELAY", EventReceiver.COLOR_ORANGE);
				receiver.drawMenuFont(engine, playerID, 1, 11, String.valueOf(engine.speed.lockDelay), (engine.statc[2] == 5));
				receiver.drawMenuFont(engine, playerID, 0, 12, "DAS", EventReceiver.COLOR_ORANGE);
				receiver.drawMenuFont(engine, playerID, 1, 13, String.valueOf(engine.speed.das), (engine.statc[2] == 6));
				receiver.drawMenuFont(engine, playerID, 0, 14, "LOAD", EventReceiver.COLOR_GREEN);
				receiver.drawMenuFont(engine, playerID, 1, 15, String.valueOf(presetNumber[playerID]), (engine.statc[2] == 7));
				receiver.drawMenuFont(engine, playerID, 0, 16, "SAVE", EventReceiver.COLOR_GREEN);
				receiver.drawMenuFont(engine, playerID, 1, 17, String.valueOf(presetNumber[playerID]), (engine.statc[2] == 8));
			} else if(engine.statc[2] < 19) {
				if(owner.replayMode == false) {
					receiver.drawMenuFont(engine, playerID, 0, ((engine.statc[2] - 9) * 2) + 1, "b",
										  (playerID == 0) ? EventReceiver.COLOR_RED : EventReceiver.COLOR_BLUE);
				}

				receiver.drawMenuFont(engine, playerID, 0,  0, "GARBAGE", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1,  1, GARBAGE_TYPE_STRING[garbageType[playerID]], (engine.statc[2] == 9));
				receiver.drawMenuFont(engine, playerID, 0,  2, "SPIN BONUS", EventReceiver.COLOR_CYAN);
				String strTSpinEnable = "";
				if(version >= 4) {
					if(tspinEnableType[playerID] == 0) strTSpinEnable = "OFF";
					if(tspinEnableType[playerID] == 1) strTSpinEnable = "T-ONLY";
					if(tspinEnableType[playerID] == 2) strTSpinEnable = "ALL";
				} else {
					strTSpinEnable = GeneralUtil.getONorOFF(enableTSpin[playerID]);
				}
				receiver.drawMenuFont(engine, playerID, 1,  3, strTSpinEnable, (engine.statc[2] == 10));
				receiver.drawMenuFont(engine, playerID, 0,  4, "EZ SPIN", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1,  5, GeneralUtil.getONorOFF(enableTSpinKick[playerID]), (engine.statc[2] == 11));
				receiver.drawMenuFont(engine, playerID, 0,  6, "B2B", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1,  7, GeneralUtil.getONorOFF(enableB2B[playerID]), (engine.statc[2] == 12));
				receiver.drawMenuFont(engine, playerID, 0,  8, "COMBO", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1,  9, GeneralUtil.getONorOFF(enableCombo[playerID]), (engine.statc[2] == 13));
				receiver.drawMenuFont(engine, playerID, 0, 10, "BIG", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1, 11, GeneralUtil.getONorOFF(big[playerID]), (engine.statc[2] == 14));
				receiver.drawMenuFont(engine, playerID, 0, 12, "SE", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1, 13, GeneralUtil.getONorOFF(enableSE[playerID]), (engine.statc[2] == 15));
				receiver.drawMenuFont(engine, playerID, 0, 14, "HURRYUP", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1, 15, (hurryupSeconds[playerID] == -1) ? "NONE" : hurryupSeconds[playerID]+"SEC",
				                      (engine.statc[2] == 16));
				receiver.drawMenuFont(engine, playerID, 0, 16, "INTERVAL", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1, 17, String.valueOf(hurryupInterval[playerID]), (engine.statc[2] == 17));
				receiver.drawMenuFont(engine, playerID, 0, 18, "BGM", EventReceiver.COLOR_PINK);
				receiver.drawMenuFont(engine, playerID, 1, 19, String.valueOf(bgmno), (engine.statc[2] == 18));
			} else {
				if(owner.replayMode == false) {
					receiver.drawMenuFont(engine, playerID, 0, ((engine.statc[2] - 19) * 2) + 1, "b",
										  (playerID == 0) ? EventReceiver.COLOR_RED : EventReceiver.COLOR_BLUE);
				}

				receiver.drawMenuFont(engine, playerID, 0,  0, "USE MAP", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1,  1, GeneralUtil.getONorOFF(useMap[playerID]), (engine.statc[2] == 19));
				receiver.drawMenuFont(engine, playerID, 0,  2, "MAP SET", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1,  3, String.valueOf(mapSet[playerID]), (engine.statc[2] == 20));
				receiver.drawMenuFont(engine, playerID, 0,  4, "MAP NO.", EventReceiver.COLOR_CYAN);
				receiver.drawMenuFont(engine, playerID, 1,  5, (mapNumber[playerID] < 0) ? "RANDOM" : mapNumber[playerID]+"/"+(mapMaxNo[playerID]-1),
									  (engine.statc[2] == 21));
			}
		} else {
			receiver.drawMenuFont(engine, playerID, 3, 10, "WAIT", EventReceiver.COLOR_YELLOW);
		}
	}

	/*
	 * Readyの時の初期化処理（初期化前）
	 */
	@Override
	public boolean onReady(GameEngine engine, int playerID) {
		if(engine.statc[0] == 0) {
			// マップ読み込み・リプレイ保存用にバックアップ
			if(version >= 3) {
				if(useMap[playerID]) {
					if(owner.replayMode) {
						engine.createFieldIfNeeded();
						loadMap(engine.field, owner.replayProp, playerID);
						engine.field.setAllSkin(engine.getSkin());
					} else {
						if(propMap[playerID] == null) {
							propMap[playerID] = receiver.loadProperties("config/map/vsbattle/" + mapSet[playerID] + ".map");
						}

						if(propMap[playerID] != null) {
							engine.createFieldIfNeeded();

							if(mapNumber[playerID] < 0) {
								if((playerID == 1) && (useMap[0]) && (mapNumber[0] < 0)) {
									engine.field.copy(owner.engine[0].field);
								} else {
									int no = (mapMaxNo[playerID] < 1) ? 0 : randMap.nextInt(mapMaxNo[playerID]);
									loadMap(engine.field, propMap[playerID], no);
								}
							} else {
								loadMap(engine.field, propMap[playerID], mapNumber[playerID]);
							}

							engine.field.setAllSkin(engine.getSkin());
							fldBackup[playerID] = new Field(engine.field);
						}
					}
				} else if(engine.field != null) {
					engine.field.reset();
				}
			}
		}

		return false;
	}

	/*
	 * ゲーム開始時の処理
	 */
	@Override
	public void startGame(GameEngine engine, int playerID) {
		engine.b2bEnable = enableB2B[playerID];
		engine.comboType = enableCombo[playerID] ? GameEngine.COMBO_TYPE_NORMAL : GameEngine.COMBO_TYPE_DISABLE;
		engine.big = big[playerID];
		engine.enableSE = enableSE[playerID];
		if(playerID == 1) owner.bgmStatus.bgm = bgmno;

		engine.tspinAllowKick = enableTSpinKick[playerID];
		if(version >= 4) {
			if(tspinEnableType[playerID] == 0) {
				engine.tspinEnable = false;
				engine.useAllSpinBonus = false;
			} else if(tspinEnableType[playerID] == 1) {
				engine.tspinEnable = true;
				engine.useAllSpinBonus = false;
			} else if(tspinEnableType[playerID] == 2) {
				engine.tspinEnable = true;
				engine.useAllSpinBonus = true;
			}
		} else {
			engine.tspinEnable = enableTSpin[playerID];
		}
	}

	/*
	 * スコア表示
	 */
	@Override
	public void renderLast(GameEngine engine, int playerID) {
		// ステータス表示
		if(playerID == 0) {
			receiver.drawScoreFont(engine, playerID, 0, 0, "VS-BATTLE", EventReceiver.COLOR_ORANGE);

			receiver.drawScoreFont(engine, playerID, 0, 2, "1P GARBAGE", EventReceiver.COLOR_RED);
			receiver.drawScoreFont(engine, playerID, 0, 3, String.valueOf(garbage[0]), (garbage[0] > 0));

			receiver.drawScoreFont(engine, playerID, 0, 5, "2P GARBAGE", EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 0, 6, String.valueOf(garbage[1]), (garbage[1] > 0));

			receiver.drawScoreFont(engine, playerID, 0, 8, "1P ATTACK", EventReceiver.COLOR_RED);
			receiver.drawScoreFont(engine, playerID, 0, 9, String.valueOf(garbageSent[0]));

			receiver.drawScoreFont(engine, playerID, 0, 11, "2P ATTACK", EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 0, 12, String.valueOf(garbageSent[1]));

			receiver.drawScoreFont(engine, playerID, 0, 14, "TIME", EventReceiver.COLOR_GREEN);
			receiver.drawScoreFont(engine, playerID, 0, 15, GeneralUtil.getTime(engine.statistics.time));

			if((hurryupSeconds[playerID] >= 0) && (engine.timerActive) &&
			   (engine.statistics.time >= hurryupSeconds[playerID] * 60) && (engine.statistics.time < (hurryupSeconds[playerID] + 5) * 60))
			{
				receiver.drawScoreFont(engine, playerID, 0, 17, "HURRY UP!", (engine.statistics.time % 2 == 0));
			}
		}

		// ライン消去イベント表示
		if((lastevent[playerID] != EVENT_NONE) && (scgettime[playerID] < 120)) {
			String strPieceName = Piece.getPieceName(lastpiece[playerID]);

			switch(lastevent[playerID]) {
			case EVENT_SINGLE:
				receiver.drawMenuFont(engine, playerID, 2, 21, "SINGLE", EventReceiver.COLOR_DARKBLUE);
				break;
			case EVENT_DOUBLE:
				receiver.drawMenuFont(engine, playerID, 2, 21, "DOUBLE", EventReceiver.COLOR_BLUE);
				break;
			case EVENT_TRIPLE:
				receiver.drawMenuFont(engine, playerID, 2, 21, "TRIPLE", EventReceiver.COLOR_GREEN);
				break;
			case EVENT_FOUR:
				if(lastb2b[playerID]) receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", EventReceiver.COLOR_RED);
				else receiver.drawMenuFont(engine, playerID, 3, 21, "FOUR", EventReceiver.COLOR_ORANGE);
				break;
			case EVENT_TSPIN_SINGLE_MINI:
				if(lastb2b[playerID]) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-S", EventReceiver.COLOR_RED);
				else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-S", EventReceiver.COLOR_ORANGE);
				break;
			case EVENT_TSPIN_SINGLE:
				if(lastb2b[playerID]) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-SINGLE", EventReceiver.COLOR_RED);
				else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-SINGLE", EventReceiver.COLOR_ORANGE);
				break;
			case EVENT_TSPIN_DOUBLE_MINI:
				if(lastb2b[playerID]) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-D", EventReceiver.COLOR_RED);
				else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-MINI-D", EventReceiver.COLOR_ORANGE);
				break;
			case EVENT_TSPIN_DOUBLE:
				if(lastb2b[playerID]) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-DOUBLE", EventReceiver.COLOR_RED);
				else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-DOUBLE", EventReceiver.COLOR_ORANGE);
				break;
			case EVENT_TSPIN_TRIPLE:
				if(lastb2b[playerID]) receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-TRIPLE", EventReceiver.COLOR_RED);
				else receiver.drawMenuFont(engine, playerID, 1, 21, strPieceName + "-TRIPLE", EventReceiver.COLOR_ORANGE);
				break;
			}

			if(lastcombo[playerID] >= 2)
				receiver.drawMenuFont(engine, playerID, 2, 22, (lastcombo[playerID] - 1) + "COMBO", EventReceiver.COLOR_CYAN);
		}
	}

	/*
	 * スコア計算
	 */
	@Override
	public void calcScore(GameEngine engine, int playerID, int lines) {
		int enemyID = 0;
		if(playerID == 0) enemyID = 1;

		// 攻撃
		if(lines > 0) {
			int pts = 0;
			scgettime[playerID] = 0;

			if(engine.tspin) {
				// T-Spin 1列
				if(lines == 1) {
					if(engine.tspinmini) {
						if(engine.useAllSpinBonus) {
							//pts += 0;
						} else {
							pts += 1;
						}
						lastevent[playerID] = EVENT_TSPIN_SINGLE_MINI;
					} else {
						pts += 2;
						lastevent[playerID] = EVENT_TSPIN_SINGLE;
					}
				}
				// T-Spin 2列
				else if(lines == 2) {
					if(engine.tspinmini && engine.useAllSpinBonus) {
						pts += 3;
						lastevent[playerID] = EVENT_TSPIN_DOUBLE_MINI;
					} else {
						pts += 4;
						lastevent[playerID] = EVENT_TSPIN_DOUBLE;
					}
				}
				// T-Spin 3列
				else if(lines >= 3) {
					pts += 6;
					lastevent[playerID] = EVENT_TSPIN_TRIPLE;
				}
			} else {
				if(lines == 1) {
					// 1列
					lastevent[playerID] = EVENT_SINGLE;
				} else if(lines == 2) {
					pts += 1; // 2列
					lastevent[playerID] = EVENT_DOUBLE;
				} else if(lines == 3) {
					pts += 2; // 3列
					lastevent[playerID] = EVENT_TRIPLE;
				} else if(lines >= 4) {
					pts += 4; // 4列
					lastevent[playerID] = EVENT_FOUR;
				}
			}

			// B2B
			if(engine.b2b) {
				lastb2b[playerID] = true;

				if(pts > 0) {
					if((version >= 1) && (lastevent[playerID] == EVENT_TSPIN_TRIPLE) && (!engine.useAllSpinBonus))
						pts += 2;
					else
						pts += 1;
				}
			} else {
				lastb2b[playerID] = false;
			}

			// コンボ
			if(engine.comboType != GameEngine.COMBO_TYPE_DISABLE) {
				int cmbindex = engine.combo - 1;
				if(cmbindex < 0) cmbindex = 0;
				if(cmbindex >= COMBO_ATTACK_TABLE.length) cmbindex = COMBO_ATTACK_TABLE.length - 1;
				pts += COMBO_ATTACK_TABLE[cmbindex];
				lastcombo[playerID] = engine.combo;
			}

			// 全消し
			if((lines >= 1) && (engine.field.isEmpty())) {
				engine.playSE("bravo");
				pts += 6;
			}

			// 宝石ブロック攻撃
			pts += engine.field.getHowManyGemClears();

			lastpiece[playerID] = engine.nowPieceObject.id;

			/*
			if(pts > 0) {
				garbageSent[playerID] += pts;

				if(garbage[playerID] > 0) {
					// 相殺
					garbage[playerID] -= pts;
					if(garbage[playerID] < 0) {
						// おじゃま返し
						garbage[enemyID] += Math.abs(garbage[playerID]);
						garbage[playerID] = 0;
					}
				} else {
					// 攻撃
					garbage[enemyID] += pts;
				}
			}
			*/

			// 攻撃ライン数
			garbageSent[playerID] += pts;

			// 相殺
			garbage[playerID] = getTotalGarbageLines(playerID);
			if((pts > 0) && (garbage[playerID] > 0)) {
				while(!garbageEntries[playerID].isEmpty() && (pts > 0)) {
					GarbageEntry garbageEntry = garbageEntries[playerID].getFirst();
					garbageEntry.lines -= pts;

					if(garbageEntry.lines <= 0) {
						pts = Math.abs(garbageEntry.lines);
						garbageEntries[playerID].removeFirst();
					} else {
						pts = 0;
					}
				}
			}

			// 攻撃
			if(pts > 0) {
				garbageEntries[enemyID].add(new GarbageEntry(pts, playerID));
				garbage[enemyID] = getTotalGarbageLines(enemyID);

				if((owner.engine[enemyID].ai == null) && (garbage[enemyID] >= 4)) {
					owner.engine[enemyID].playSE("danger");
				}
			}
		}

		// せり上がり
		garbage[playerID] = getTotalGarbageLines(playerID);
		if((lines == 0) && (garbage[playerID] > 0)) {
			engine.playSE("garbage");

			while(!garbageEntries[playerID].isEmpty()) {
				GarbageEntry garbageEntry = garbageEntries[playerID].poll();
				int garbageColor = PLAYER_COLOR_BLOCK[garbageEntry.playerID];

				if(garbageEntry.lines > 0) {
					if(garbageType[playerID] == GARBAGE_TYPE_NORMAL) {
						// ノーマルな穴位置変更
						int hole = engine.random.nextInt(engine.field.getWidth());

						while(garbageEntry.lines > 0) {
							engine.field.addSingleHoleGarbage(hole, garbageColor, engine.getSkin(),
									  Block.BLOCK_ATTRIBUTE_GARBAGE | Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_OUTLINE,
									  1);

							if(engine.random.nextInt(10) >= 7) {
								hole = engine.random.nextInt(engine.field.getWidth());
							}

							garbageEntry.lines--;
						}
					} else if(garbageType[playerID] == GARBAGE_TYPE_NOCHANGE_ONE_RISE) {
						// 1回のせり上がりで穴位置が変わらない
						int hole = engine.random.nextInt(engine.field.getWidth());
						engine.field.addSingleHoleGarbage(hole, garbageColor, engine.getSkin(),
														  Block.BLOCK_ATTRIBUTE_GARBAGE | Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_OUTLINE,
														  garbage[playerID]);
						garbageEntries[playerID].clear();
						break;
					} else if(garbageType[playerID] == GARBAGE_TYPE_NOCHANGE_ONE_ATTACK) {
						// 邪魔ブロックの穴の位置が1回の攻撃で変わらない(2回以上なら変わる)
						int hole = engine.random.nextInt(engine.field.getWidth());
						engine.field.addSingleHoleGarbage(hole, garbageColor, engine.getSkin(),
														  Block.BLOCK_ATTRIBUTE_GARBAGE | Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_OUTLINE,
														  garbageEntry.lines);
					}
				}
			}

			garbage[playerID] = 0;
		}

		// HURRY UP!
		if(version >= 2) {
			if((hurryupSeconds[playerID] >= 0) && (engine.timerActive)) {
				if(engine.statistics.time >= hurryupSeconds[playerID] * 60) {
					hurryupCount[playerID]++;

					if(hurryupCount[playerID] % hurryupInterval[playerID] == 0) {
						engine.field.addHurryupFloor(1, engine.getSkin());
					}
				} else {
					hurryupCount[playerID] = hurryupInterval[playerID] - 1;
				}
			}
		} else {
			if((hurryupSeconds[playerID] >= 0) && (engine.timerActive) && (engine.statistics.time >= hurryupSeconds[playerID] * 60)) {
				hurryupCount[playerID]++;

				if(hurryupCount[playerID] % hurryupInterval[playerID] == 0) {
					engine.field.addHurryupFloor(1, engine.getSkin());
				}
			}
		}
	}

	/*
	 * 各フレームの最後の処理
	 */
	@Override
	public void onLast(GameEngine engine, int playerID) {
		scgettime[playerID]++;

		// HURRY UP!
		if((playerID == 0) && (engine.timerActive) && (hurryupSeconds[playerID] >= 0) && (engine.statistics.time == hurryupSeconds[playerID] * 60)) {
			owner.receiver.playSE("hurryup");
		}

		// せり上がりメーター
		if(garbage[playerID] * receiver.getBlockGraphicsHeight(engine, playerID) > engine.meterValue) {
			engine.meterValue += receiver.getBlockGraphicsHeight(engine, playerID) / 2;
		} else if(garbage[playerID] * receiver.getBlockGraphicsHeight(engine, playerID) < engine.meterValue) {
			engine.meterValue--;
		}
		if(garbage[playerID] >= 4) engine.meterColor = GameEngine.METER_COLOR_RED;
		else if(garbage[playerID] >= 3) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
		else if(garbage[playerID] >= 1) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
		else engine.meterColor = GameEngine.METER_COLOR_GREEN;

		// 決着
		if((playerID == 1) && (owner.engine[0].gameActive)) {
			if((owner.engine[0].stat == GameEngine.STAT_GAMEOVER) && (owner.engine[1].stat == GameEngine.STAT_GAMEOVER)) {
				// 引き分け
				winnerID = -1;
				owner.engine[0].gameActive = false;
				owner.engine[1].gameActive = false;
				owner.bgmStatus.bgm = BGMStatus.BGM_NOTHING;
			} else if((owner.engine[0].stat != GameEngine.STAT_GAMEOVER) && (owner.engine[1].stat == GameEngine.STAT_GAMEOVER)) {
				// 1P勝利
				winnerID = 0;
				owner.engine[0].gameActive = false;
				owner.engine[1].gameActive = false;
				owner.engine[0].stat = GameEngine.STAT_EXCELLENT;
				owner.engine[0].resetStatc();
				owner.engine[0].statc[1] = 1;
				owner.bgmStatus.bgm = BGMStatus.BGM_NOTHING;
			} else if((owner.engine[0].stat == GameEngine.STAT_GAMEOVER) && (owner.engine[1].stat != GameEngine.STAT_GAMEOVER)) {
				// 2P勝利
				winnerID = 1;
				owner.engine[0].gameActive = false;
				owner.engine[1].gameActive = false;
				owner.engine[1].stat = GameEngine.STAT_EXCELLENT;
				owner.engine[1].resetStatc();
				owner.engine[1].statc[1] = 1;
				owner.bgmStatus.bgm = BGMStatus.BGM_NOTHING;
			}
		}
	}

	/*
	 * 結果画面の描画
	 */
	@Override
	public void renderResult(GameEngine engine, int playerID) {
		receiver.drawMenuFont(engine, playerID, 0, 1, "RESULT", EventReceiver.COLOR_ORANGE);
		if(winnerID == -1) {
			receiver.drawMenuFont(engine, playerID, 6, 2, "DRAW", EventReceiver.COLOR_GREEN);
		} else if(winnerID == playerID) {
			receiver.drawMenuFont(engine, playerID, 6, 2, "WIN!", EventReceiver.COLOR_YELLOW);
		} else {
			receiver.drawMenuFont(engine, playerID, 6, 2, "LOSE", EventReceiver.COLOR_WHITE);
		}

		receiver.drawMenuFont(engine, playerID, 0, 3, "ATTACK", EventReceiver.COLOR_ORANGE);
		String strScore = String.format("%10d", garbageSent[playerID]);
		receiver.drawMenuFont(engine, playerID, 0, 4, strScore);

		receiver.drawMenuFont(engine, playerID, 0, 5, "LINE", EventReceiver.COLOR_ORANGE);
		String strLines = String.format("%10d", engine.statistics.lines);
		receiver.drawMenuFont(engine, playerID, 0, 6, strLines);

		receiver.drawMenuFont(engine, playerID, 0, 7, "PIECE", EventReceiver.COLOR_ORANGE);
		String strPiece = String.format("%10d", engine.statistics.totalPieceLocked);
		receiver.drawMenuFont(engine, playerID, 0, 8, strPiece);

		receiver.drawMenuFont(engine, playerID, 0, 9, "ATTACK/MIN", EventReceiver.COLOR_ORANGE);
		float apm = (float)(garbageSent[playerID] * 3600) / (float)(engine.statistics.time);
		String strAPM = String.format("%10g", apm);
		receiver.drawMenuFont(engine, playerID, 0, 10, strAPM);

		receiver.drawMenuFont(engine, playerID, 0, 11, "LINE/MIN", EventReceiver.COLOR_ORANGE);
		String strLPM = String.format("%10g", engine.statistics.lpm);
		receiver.drawMenuFont(engine, playerID, 0, 12, strLPM);

		receiver.drawMenuFont(engine, playerID, 0, 13, "PIECE/SEC", EventReceiver.COLOR_ORANGE);
		String strPPS = String.format("%10g", engine.statistics.pps);
		receiver.drawMenuFont(engine, playerID, 0, 14, strPPS);

		receiver.drawMenuFont(engine, playerID, 0, 15, "TIME", EventReceiver.COLOR_ORANGE);
		String strTime = String.format("%10s", GeneralUtil.getTime(owner.engine[0].statistics.time));
		receiver.drawMenuFont(engine, playerID, 0, 16, strTime);
	}

	/*
	 * リプレイ保存時の処理
	 */
	@Override
	public void saveReplay(GameEngine engine, int playerID, CustomProperties prop) {
		saveOtherSetting(engine, owner.replayProp);
		savePreset(engine, owner.replayProp, -1 - playerID);

		if(useMap[playerID] && (fldBackup[playerID] != null)) {
			saveMap(fldBackup[playerID], owner.replayProp, playerID);
		}

		owner.replayProp.setProperty("vsbattle.version", version);
	}

	/**
	 * 敵から送られてきた邪魔ブロックのデータ
	 */
	private class GarbageEntry {
		/** 邪魔ブロック数 */
		public int lines = 0;

		/** 送信元 */
		public int playerID = 0;

		/**
		 * コンストラクタ
		 */
		@SuppressWarnings("unused")
		public GarbageEntry() {
		}

		/**
		 * パラメータ付きコンストラクタ
		 * @param g 邪魔ブロック数
		 */
		@SuppressWarnings("unused")
		public GarbageEntry(int g) {
			lines = g;
		}

		/**
		 * パラメータ付きコンストラクタ
		 * @param g 邪魔ブロック数
		 * @param p 送信元
		 */
		public GarbageEntry(int g, int p) {
			lines = g;
			playerID = p;
		}
	}
}