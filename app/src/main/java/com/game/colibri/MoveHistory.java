package com.game.colibri;

import android.os.Handler;
import android.os.Message;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.TranslateAnimation;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

class MoveHistory {

	private static final int ANIM_DELAY = 800; // en ms

	static class MapChange {

		int row;
		int col;
		int prevValue;

		MapChange(int r, int c, int prevVal) {
			row = r;
			col = c;
			prevValue = prevVal;
		}
	}

	static class AnimalState {

		private int chkpt;
		private double step;
		private int[] dir;
		private double[] pos;

		AnimalState(Animal a) {
			chkpt = 0;
			step = a.step;
			dir = a.getDirection();
			pos = a.getPos();
			if(a instanceof Vache) {
				chkpt = ((Vache) a).chkpt;
			} else if(a instanceof Chat) {
				chkpt = ((Chat) a).chkpt;
			}
		}

		void restoreState(Animal a) {
			double[] curPos = a.getPos();
			a.step = step;
			a.setDirection(dir);
			a.setPos(pos[0], pos[1]);
			if(a instanceof Vache) {
				((Vache) a).chkpt = chkpt;
			} else if(a instanceof Chat) {
				((Chat) a).chkpt = chkpt;
			}
			TranslateAnimation anim = new TranslateAnimation((float)((curPos[0]-pos[0])*Carte.cw),0,(float)((curPos[1]-pos[1])*Carte.ch),0);
			anim.setDuration(ANIM_DELAY);
			anim.setInterpolator(new AccelerateDecelerateInterpolator());
			a.setAnimation(anim);
		}
	}

	static class Move {

		private int startFrame;
		private int n_fleur, n_dyna;
		private int wait, directionDyna;
		private int[] lastMove = new int[] {0,0};
		private AnimalState colibri;
		private LinkedList<AnimalState> vaches = new LinkedList<>();
		private LinkedList<AnimalState> chats = new LinkedList<>();
		private LinkedList<MapChange> changes = new LinkedList<>();
		private boolean droppedDyna = false;

		Move(MoteurJeu mj) {
			startFrame = mj.frame;
			n_fleur = mj.carte.n_fleur;
			n_dyna = mj.carte.n_dyna;
			wait = mj.wait;
			directionDyna = mj.directionDyna;
			lastMove[0] = mj.lastMove[0];
			lastMove[1] = mj.lastMove[1];
			colibri = new AnimalState(mj.carte.colibri);
			for(Vache v : mj.carte.vaches) {
				vaches.addLast(new AnimalState(v));
			}
			for(Chat c : mj.carte.chats) {
				chats.addLast(new AnimalState(c));
			}
		}

		double[] getInitPos() {
			return colibri.pos;
		}

		void addChange(int r, int c, int prevVal) {
			changes.addFirst(new MapChange(r, c, prevVal));
		}

		Move addChangesFromMove(Move m) {
			changes.addAll(0, m.changes);
			return this;
		}

		void dropDyna() {
			droppedDyna = true;
		}

		/**
		 * Annule le dernier mouvement et restaure le jeu dans son état à frame = startFrame.
		 * @param mj le moteur de jeu
		 * @param animationHandler le Handler pour gérer l'animation
		 */
		void cancelMove(MoteurJeu mj, AnimationHandler animationHandler) {
			mj.pause(MoteurJeu.PAUSED);
			animationHandler.resumeGameAfter(ANIM_DELAY);
			int changeDelayIncrement = ANIM_DELAY/(changes.size()+1);
			int changeDelay = changeDelayIncrement;
			// Rétablir carte statique
			for(MapChange mc : changes) {
				animationHandler.animChangeAfter(mc, changeDelay);
				changeDelay += changeDelayIncrement;
			}
			// Repositionnement du menhir rouge
			mj.removeMenhirRouge(null);
			animationHandler.animMenhirRougeAfter(ANIM_DELAY);
			// Supprimer la dynamite posée pendant ce Move
			if(droppedDyna) {
				mj.carte.cancelLastDynamite();
			}
			// Mettre à jour bouton dyna
			mj.updateDynaButton(n_dyna);
			// Repositionnement des animaux
			Iterator<AnimalState> vachesStateIter = vaches.iterator();
			for(Vache v : mj.carte.vaches) {
				vachesStateIter.next().restoreState(v);
			}
			Iterator<AnimalState> chatsStateIter = chats.iterator();
			for(Chat c : mj.carte.chats) {
				chatsStateIter.next().restoreState(c);
			}
			mj.carte.colibri.setDirection(lastMove);
			mj.carte.colibri.setSpriteDirection();
			colibri.restoreState(mj.carte.colibri);
			// Retour à la frame du début (mais en comptant le temps perdu dans total_frames)
			mj.total_frames += mj.frame - startFrame;
			mj.frame = startFrame;
			mj.lastMoveFrame = startFrame;
			mj.lastFlowerFrame = startFrame;
			// Autres variables d'état
			mj.carte.n_fleur = n_fleur;
			mj.carte.n_dyna = n_dyna;
			mj.wait = wait;
			mj.directionDyna = directionDyna;
			mj.lastMove[0] = lastMove[0];
			mj.lastMove[1] = lastMove[1];
			mj.isMoving = false;
			mj.buf.clear();
		}
	}

	private static class AnimationHandler extends Handler {

		private static final int RESUME_GAME = 0, ANIM_CHANGE = 1, ANIM_MENHIR_ROUGE = 2;
		private WeakReference<MoteurJeu> mjRef;
		private int queuedCancel = 0;

		AnimationHandler(MoteurJeu moteurJeu) {
			mjRef = new WeakReference<>(moteurJeu);
		}

		void reset() {
			queuedCancel = 0;
			removeMessages(RESUME_GAME);
			removeMessages(ANIM_CHANGE);
			removeMessages(ANIM_MENHIR_ROUGE);
		}

		void queueCancelLastMove() {
			if(queuedCancel++ == 0 && mjRef.get() != null) {
				mjRef.get().moveHistory.cancelLastMove();
			}
		}

		int getQueuedCancellations() {
			return queuedCancel;
		}

		void resumeGameAfter(long delayMillis) {
			removeMessages(RESUME_GAME);
			sendEmptyMessageDelayed(RESUME_GAME, delayMillis);
		}

		void animChangeAfter(MapChange mc, long delayMillis) {
			sendMessageDelayed(obtainMessage(ANIM_CHANGE, mc), delayMillis);
		}

		void animMenhirRougeAfter(long delayMillis) {
			removeMessages(ANIM_MENHIR_ROUGE);
			sendEmptyMessageDelayed(ANIM_MENHIR_ROUGE, delayMillis);
		}

		@Override
		public void handleMessage(Message msg) {
			MoteurJeu mj = mjRef.get();
			if(mj == null)
				return;
			switch(msg.what) {
				case RESUME_GAME:
					if(--queuedCancel > 0) {
						mj.moveHistory.cancelLastMove();
					} else if(mj.state == MoteurJeu.PAUSED) {
						mj.start();
					}
					break;
				case ANIM_CHANGE:
					MapChange mc = (MapChange) msg.obj;
					mj.niv.carte[mc.row][mc.col] = mc.prevValue;
					mj.carte.fond.invalidate(); // Rafraîchir carte
					break;
				case ANIM_MENHIR_ROUGE:
					if(mj.carte.n_dyna > 0) {
						int nl = mj.carte.colibri.getRow() + mj.lastMove[1];
						int nc = mj.carte.colibri.getCol() + mj.lastMove[0];
						boolean outOfMap = nl<0 || nl>=Carte.LIG || nc<0 || nc>=Carte.COL;
						if(!outOfMap && mj.niv.carte[nl][nc] == MoteurJeu.MENHIR) {
							mj.niv.carte[nl][nc] = MoteurJeu.MENHIR_ROUGE;
							mj.carte.fond.invalidate(); // Rafraîchir carte
						}
					}
			}
		}
	}

	private MoteurJeu mj;
	private AnimationHandler animationHandler;
	private LinkedList<Move> moves = new LinkedList<>();

	MoveHistory(MoteurJeu mj) {
		this.mj = mj;
		animationHandler = new AnimationHandler(mj);
	}

	void startNextMove() {
		moves.addLast(new Move(mj));
	}

	void dropDynaAndStartNextMove() {
		moves.getLast().dropDyna();
		startNextMove();
	}

	void addChange(int r, int c, int prevVal) {
		moves.getLast().addChange(r, c, prevVal);
	}

	void reset() {
		moves.clear();
		animationHandler.reset();
		startNextMove();
	}

	void cancelLastMove() {
		Move lastMove = moves.pollLast();
		double[] initPos = lastMove.getInitPos();
		double[] currentPos = mj.carte.colibri.getPos();
		// Si le colibri a bougé, on annule juste le Move en cours, sinon celui d'avant aussi.
		if(Arrays.equals(initPos, currentPos) && !moves.isEmpty()) { // Le colibri n'a pas bougé
			lastMove = moves.pollLast().addChangesFromMove(lastMove);
		}
		lastMove.cancelMove(mj, animationHandler);
		startNextMove(); // Nouveau Move courant
	}

	void queueCancelLastMove() {
		int queuedCancel = animationHandler.getQueuedCancellations();
		if(queuedCancel < moves.size())
			animationHandler.queueCancelLastMove();
	}
}
