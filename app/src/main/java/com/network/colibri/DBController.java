package com.network.colibri;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.game.colibri.Defi;
import com.game.colibri.Joueur;
import com.game.colibri.Participation;
import com.game.colibri.R;
import com.google.gson.Gson;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseArray;

public class DBController  extends SQLiteOpenHelper {
	
	public DBController(Context applicationcontext) {
        super(applicationcontext, "Colibri.db", null, 11);
    }
	
	//Creates Tables
	@Override
	public void onCreate(SQLiteDatabase database) {
		String query;
		query = "CREATE TABLE IF NOT EXISTS defis ("
				+ " id int NOT NULL,"
				+ " nom varchar(25) DEFAULT NULL,"
				+ " nMatch int NOT NULL,"
				+ " nivCours text DEFAULT NULL,"
				+ " nivFini text DEFAULT NULL,"
				+ " t_max int NOT NULL,"
				+ " limite int NOT NULL,"
				+ " type int NOT NULL,"
				+ " resVus int NOT NULL,"
				+ " PRIMARY KEY (`id`)"
				+ ")";
        database.execSQL(query);
        query = "CREATE TABLE IF NOT EXISTS joueurs ("
        		+ " id int NOT NULL,"
        		+ " pseudo varchar(15) NOT NULL,"
        		+ " pays varchar(10) DEFAULT NULL,"
				+ " exp int NOT NULL DEFAULT 0,"
				+ " progress int NOT NULL DEFAULT 1,"
				+ " coliBrains int NOT NULL DEFAULT 0,"
				+ " expProgCB int NOT NULL DEFAULT 0,"
				+ " defis int NOT NULL DEFAULT 0,"
				+ " win int NOT NULL DEFAULT 0,"
				+ " loose int NOT NULL DEFAULT 0,"
				+ " score real NOT NULL DEFAULT 0,"
				+ " playTime int NOT NULL DEFAULT 0,"
				+ " avatar int NOT NULL DEFAULT 0,"
				+ " time int NOT NULL DEFAULT 0,"
				+ " PRIMARY KEY (pseudo)"
				+ ")";
        database.execSQL(query);
        query = "CREATE TABLE IF NOT EXISTS participations ("
        		+ " defi int NOT NULL,"
        		+ " joueur int NOT NULL,"
				+ " cumul_score real NOT NULL DEFAULT 0,"
				+ " t_cours int NOT NULL DEFAULT 0,"
				+ " t_fini int NOT NULL DEFAULT 0,"
				+ " score real NOT NULL DEFAULT 0,"
				+ " rank int NOT NULL DEFAULT 0,"
				+ " PRIMARY KEY (defi, joueur),"
				+ "FOREIGN KEY(defi) REFERENCES defis(id) ON DELETE CASCADE ON UPDATE CASCADE,"
				+ "FOREIGN KEY(joueur) REFERENCES joueurs(pseudo) ON DELETE CASCADE ON UPDATE CASCADE"
				+ ")";
        database.execSQL(query);
        query = "CREATE TABLE IF NOT EXISTS tasks ("
				+ " id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ " task text NOT NULL"
				+ ")";
        database.execSQL(query);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase database, int version_old, int current_version) {
		String query;
		query = "DROP TABLE IF EXISTS defis";
		database.execSQL(query);
		query = "DROP TABLE IF EXISTS joueurs";
		database.execSQL(query);
		query = "DROP TABLE IF EXISTS participations";
		database.execSQL(query);
		query = "DROP TABLE IF EXISTS tasks";
		database.execSQL(query);
        onCreate(database);
	}
	
	/**
	 * 
	 * @param jsonArray [{id:2,nMatch:4, ...},{},...]
	 */
	public void insertJSONDefis(JSONArray jsonArray) {
		SQLiteDatabase database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		for(int i=0; i<jsonArray.length(); i++) {
			try {
				JSONObject d = jsonArray.getJSONObject(i);
				values.put("id", d.getInt("id"));
				values.put("nom",d.getString("nom"));
				values.put("nMatch",d.getInt("nMatch"));
				values.put("nivCours",d.isNull("nivCours") ? null : d.getString("nivCours"));
				values.put("nivFini",d.isNull("nivFini") ? null : d.getString("nivFini"));
				values.put("t_max", d.getInt("t_max"));
				values.put("limite", d.getInt("t_restant")+System.currentTimeMillis()/1000);
				values.put("type", d.getInt("type"));
				values.put("resVus", d.getInt("resVus"));
				database.insertWithOnConflict("defis", null, values, SQLiteDatabase.CONFLICT_REPLACE);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		database.close();
	}
	
	public void insertJSONParticipations(JSONArray jsonArray) {
		SQLiteDatabase database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		for(int i=0; i<jsonArray.length(); i++) {
			try {
				JSONObject d = jsonArray.getJSONObject(i);
				values.put("defi",d.getInt("defi"));
				values.put("joueur",d.getString("joueur"));
				values.put("cumul_score",d.getDouble("cumul_score"));
				values.put("t_cours",d.getInt("t_cours"));
				values.put("t_fini",d.getInt("t_fini"));
				values.put("score",d.getDouble("score"));
				values.put("rank",d.getInt("rank"));
				database.insertWithOnConflict("participations", null, values, SQLiteDatabase.CONFLICT_REPLACE);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		database.close();
	}
	
	public void insertJSONJoueurs(JSONArray jsonArray) {
		SQLiteDatabase database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		for(int i=0; i<jsonArray.length(); i++) {
			try {
				JSONObject d = jsonArray.getJSONObject(i);
				values.put("id",d.getString("id"));
				values.put("pseudo",d.getString("pseudo"));
				values.put("pays",d.getString("pays"));
				values.put("exp",d.getInt("exp"));
				values.put("progress",d.getInt("progress"));
				values.put("coliBrains",d.getInt("coliBrains"));
				values.put("expProgCB",d.getInt("expProgCB"));
				values.put("defis",d.getInt("defis"));
				values.put("win",d.getInt("win"));
				values.put("loose",d.getInt("loose"));
				values.put("score",d.getDouble("score"));
				values.put("playTime",d.getInt("playTime"));
				values.put("avatar",d.getInt("avatar"));
				values.put("time",System.currentTimeMillis()/1000 - d.getLong("time"));
				database.insertWithOnConflict("joueurs", null, values, SQLiteDatabase.CONFLICT_REPLACE);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		database.close();
	}
	
	/**
	 * Efface le contenu de la base de donnée (lors d'une déconnexion).
	 */
	public void clearDB() {
		SQLiteDatabase database = this.getWritableDatabase();
		database.delete("defis", null, null);
		database.delete("joueurs", null, null);
		database.delete("participations", null, null);
		database.delete("tasks", null, null);
		database.close();
	}
	
	/**
	 * Retourne les tâches en attente à envoyer au serveur sous cette forme :
	 * [{"task":"finMatch", "id_defi":_, "pseudo":_, "temps":_, "vainqueur":_, "exp_v":_, "exp_p":_},
	 *  {"task":"solvedMatch", "id_defi":_, "pseudo":_, "temps":_}, {"task":"nouveauMatch", ...}]
	 * @return le string JSON des tâches
	 */
	public String getTasks() {
		JSONArray tasks = new JSONArray();
		JSONObject o;
	    SQLiteDatabase database = this.getWritableDatabase();
	    Cursor cursor = database.rawQuery("SELECT task, id FROM `tasks` ORDER BY id", null);
	    if(cursor.moveToFirst()) {
	        do {
	        	try {
		        	o = new JSONObject(cursor.getString(0));
		        	o.put("t_id", cursor.getInt(1));
					tasks.put(o);
	        	} catch (JSONException e) {
					e.printStackTrace();
				}
	        } while(cursor.moveToNext());
	    }
	    cursor.close();
	    database.close();
		return tasks.toString();
	}
	
	/**
	 * Une fois que les tâches ont été envoyées au serveur, supprime les tâches.
	 */
	public void clearTasks() {
		SQLiteDatabase database = this.getWritableDatabase();
		database.delete("tasks", null, null);
		//database.delete("sqlite_sequence", "name='tasks'", null);
		database.close();
	}
	
	/**
	 * Remplit la SparseArray des joueurs impliqués dans les défis.
	 */
	public void getJoueurs(SparseArray<Joueur> joueurs) {
		joueurs.clear();
		String selectQuery = "SELECT id,pseudo,pays,exp,progress,coliBrains,expProgCB,defis,win,loose,score,playTime,avatar,time FROM `joueurs`";
	    SQLiteDatabase database = this.getWritableDatabase();
	    Cursor cursor = database.rawQuery(selectQuery, null);
	    if (cursor.moveToFirst()) {
	        do {
	        	joueurs.put(cursor.getInt(0), new Joueur(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6), cursor.getInt(7), cursor.getInt(8), cursor.getInt(9), cursor.getDouble(10), cursor.getLong(11), cursor.getInt(12), cursor.getLong(13)));
	        } while(cursor.moveToNext());
	    }
	    cursor.close();
	    database.close();
	}
	
	/**
	 * Retourne la liste des défis en ordonnant en premier les défis qui ne sont pas en Attente.
	 * @param user le nom de l'utilisateur
	 * @param joueurs les joueurs
	 * @return la liste des défis à afficher
	 */
	public int getDefis(int user, SparseArray<Joueur> joueurs, ArrayList<Defi> l) {
		int pRapideIndice = -1; // Pour lancer la partie rapide juste reçue le cas échéant.
		l.clear();
		String selectQuery = ""
				+ "SELECT id,nom,d.nMatch,nivCours,nivFini,t_max,limite,type,resVus,"
				+ "(CASE WHEN nivFini IS NOT NULL AND d.nMatch<>resVus THEN 1 ELSE (CASE WHEN t_cours>0 THEN 3 ELSE (CASE WHEN nivCours IS NULL THEN 2 ELSE 0 END) END) END) AS ordre "
				+ "FROM `defis` AS d JOIN `participations` AS p ON id=p.defi WHERE joueur=? ORDER BY ordre, id";
	    SQLiteDatabase database = this.getWritableDatabase();
	    Cursor cursor = database.rawQuery(selectQuery, new String[] {""+user});
	    if (cursor.moveToFirst()) {
	        do {
	        	String selectQuery2 = "SELECT joueur,cumul_score,t_cours,t_fini,score,rank FROM `participations` WHERE defi="+cursor.getInt(0);
	        	Cursor cursor2 = database.rawQuery(selectQuery2, null);
	        	SparseArray<Participation> part = new SparseArray<>(cursor2.getCount());
	        	if(cursor2.moveToFirst()) {
		        	do {
		        		Participation p = new Participation(joueurs.get(cursor2.getInt(0)),cursor2.getDouble(1),cursor2.getInt(2),cursor2.getInt(3),cursor2.getDouble(4),cursor2.getInt(5));
		        		part.put(cursor2.getInt(0), p);
		        	} while(cursor2.moveToNext());
	        	}
	        	cursor2.close();
	        	if(cursor.getInt(7)>0 && cursor.getInt(9)%2==0) // Partie rapide non jouée (donc à lancer) (type>0 && etat=Lancer ou Relever)
	        		pRapideIndice = l.size();
	        	l.add(new Defi(cursor.getInt(0), cursor.getString(1), part, cursor.getInt(2), cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getInt(6), cursor.getInt(7), cursor.getInt(8)));
	        } while (cursor.moveToNext());
	    }
	    cursor.close();
	    database.close();
	    return pRapideIndice;
	}
	
	/**
	 * Supprime les Joueurs qui n'ont aucune participation dans les défis de l'utilisateur.
	 * @param database la bdd
	 */
	private void cleanJoueurs(SQLiteDatabase database, int user) {
		//database.execSQL("DELETE FROM `joueurs` WHERE pseudo NOT IN (SELECT joueur FROM Participations) AND pseudo<>'"+user+"'");
		database.delete("joueurs", "id NOT IN (SELECT joueur FROM Participations) AND pseudo<>"+user, null);
	}
	
	/**
	 * Supprimer un défi et retirer sa participation.
	 * @param defi le défi
	 * @param user l'ID joueur de l'utilisateur
	 */
	public void removeDefi(Defi defi, int user) {
		SQLiteDatabase database = this.getWritableDatabase();
		database.delete("defis", "id="+defi.id, null);
		database.delete("participations", "defi="+defi.id, null);
		cleanJoueurs(database, user);
		// Requête serveur
		ContentValues values = new ContentValues();
		JSONObject o = new JSONObject();
		try {
			o.put("task","removeParticip");
			o.put("defi",defi.id);
			values.put("task", o.toString());
			database.insert("tasks", null, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		database.close();
	}

	/**
	 * Stocker le nouveau défi et envoyer maj pour seulement le défi.
	 * @param defi le défi
	 */
	public void updateDefi(Defi defi) {
		Gson g = new Gson();
		SQLiteDatabase database = this.getWritableDatabase();
		String m = g.toJson(defi.match);
		// Stocker le nouveau match
		ContentValues values = new ContentValues();
		values.put("nivCours", m);
		values.put("limite", defi.limite);
		database.update("defis", values, "id="+defi.id, null);
		// Mettre en queue une requête de nouveau match
		values = new ContentValues();
		JSONObject o = new JSONObject();
		try {
			o.put("task","nouveauMatch");
			o.put("defi",defi.id);
			o.put("nivCours",m);
			values.put("task", o.toString());
			database.insert("tasks", null, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		database.close();
	}

	/**
	 * Stocker les modifications dans la BDD + envoyer une maj pour le defi, les participations
	 * ET les joueurs.
	 * @param defi le défi
	 * @param user l'id joueur de l'utilisateur
	 * @param nMatch l'indice nMatch
	 */
	public void updateDefiTout(Defi defi, int user, int nMatch) {
		Gson g = new Gson();
		SQLiteDatabase database = this.getWritableDatabase();
		// MAJ Defi
		ContentValues values = new ContentValues();
		values.put("nMatch", defi.nMatch);
		values.put("nivCours", defi.match==null ? null : g.toJson(defi.match));
		values.put("nivFini", defi.matchFini==null ? null : g.toJson(defi.matchFini));
		database.update("defis", values, "id="+defi.id, null);
		for(int i=0, length=defi.participants.size(); i<length; i++) {
			Participation p = defi.participants.valueAt(i);
			// MAJ Participations
			values = new ContentValues();
			values.put("cumul_score", p.cumul_score);
			values.put("t_cours", p.t_cours);
			values.put("t_fini", p.t_fini);
			values.put("score", p.score);
			values.put("rank", p.rank);
			database.update("participations", values, "defi="+defi.id+" AND joueur="+p.joueur.getId(), null);
			// MAJ Joueurs
			values = new ContentValues();
			values.put("exp", p.joueur.getExp());
			values.put("defis", p.joueur.getDefis());
			values.put("win", p.joueur.getWin());
			values.put("loose", p.joueur.getLoose());
			values.put("score", p.joueur.getScore());
			values.put("playTime", p.joueur.getPlayTime());
			database.update("joueurs", values, "pseudo="+p.joueur.getId(), null);
		}
		// Requête serveur
		values = new ContentValues();
		JSONObject o = new JSONObject();
		try {
			Participation p = defi.participants.get(user);
			o.put("task","finMatch");
			o.put("defi",defi.id);
			o.put("temps",(p.t_cours!=0) ? p.t_cours : p.t_fini);
			o.put("nMatch", nMatch);
			values.put("task", o.toString());
			database.insert("tasks", null, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		database.close();
	}

	/**
	 * Stocker les modifications dans la BDD + envoyer une maj pour la participation SEULEMENT.
	 * @param p la participation
	 * @param defi l'id du défi
	 * @param nMatch l'indice nMatch
	 */
	public void updateParticipation(Participation p, int defi, int nMatch) {
		SQLiteDatabase database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("cumul_score", p.cumul_score);
		values.put("t_cours", p.t_cours);
		values.put("t_fini", p.t_fini);
		values.put("score", p.score);
		values.put("rank", p.rank);
		database.update("participations", values, "defi="+defi+" AND joueur="+p.joueur.getId(), null);
		// Requète Serveur
		values = new ContentValues();
		JSONObject o = new JSONObject();
		try {
			o.put("task","finMatch");
			o.put("defi",defi);
			o.put("temps",(p.t_cours!=0) ? p.t_cours : p.t_fini);
			o.put("nMatch", nMatch);
			values.put("task", o.toString());
			database.insert("tasks", null, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		database.close();
	}
	
	/**
	 * Update la participation de user à defi avec Forfait. (Utilisé lorsque le défi a été fuit en forçant la fermeture du jeu)
	 * @param defi id du défi
	 * @param user id du joueur
	 */
	public void forfaitDefi(int defi, int user) {
		SQLiteDatabase database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("t_cours", Participation.FORFAIT);
		database.update("participations", values, "defi="+defi+" AND joueur="+user, null);
		// Requète serveur
		values = new ContentValues();
		JSONObject o = new JSONObject();
		Cursor cursor = database.rawQuery("SELECT nMatch FROM `defis` WHERE id="+defi, null);
		cursor.moveToFirst();
		try {
			o.put("task","finMatch");
			o.put("defi",defi);
			o.put("temps",Participation.FORFAIT);
			o.put("nMatch", cursor.getInt(0));
			values.put("task", o.toString());
			database.insert("tasks", null, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		cursor.close();
		database.close();
	}
	
	/**
	 * Enregistre que les résultats du défi ont été vus pour le match nMatch puis notifie le serveur.
	 * @param id le défi
	 * @param nMatch la valeur
	 */
	public void setResultatsVus(int id, int nMatch) {
		SQLiteDatabase database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("resVus", nMatch);
		database.update("defis", values, "id="+id, null);
		// Requète Serveur
		values = new ContentValues();
		JSONObject o = new JSONObject();
		try {
			o.put("task","resultatsVus");
			o.put("defi",id);
			o.put("nMatch", nMatch);
			values.put("task", o.toString());
			database.insert("tasks", null, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		database.close();
	}
	
	/**
	 * Crée une tâche serveur pour un nouveau défi.
	 * @param nomDefi le nom du défi
	 * @param participants la liste des participants
	 * @param t_max le temps de vie de chaque match (int en String)
	 */
	public void newDefi(String nomDefi, JSONArray participants, int t_max) {
		SQLiteDatabase database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		JSONObject o = new JSONObject();
		try {
			o.put("task", "newDefi");
			o.put("nom", nomDefi);
			o.put("participants", participants);
			o.put("t_max", t_max);
			values.put("task", o.toString());
			database.insert("tasks", null, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		database.close();
	}
	
	/**
	 * Crée une tâche serveur pour une nouvelle partie rapide.
	 */
	public void partieRapide() {
		SQLiteDatabase database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		JSONObject o = new JSONObject();
		try {
			o.put("task", "partieRapide");
			values.put("task", o.toString());
			database.insert("tasks", null, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		database.close();
	}
	
	/**
	 * Crée une tâche serveur pour ajouter exp en expérience et mettre à jour les ColiBrains
	 * et l'expérience associée.
	 * Côté serveur :
	 * expProgCB += cumulExpColiBrains;
	 * coliBrains = min(coliBrains-usedColiBrains+expProgCB/EXP_LEVEL_PER_COLI_BRAIN, maxCB);
	 * expProgCB = coliBrains==maxCB ? 0 : expProgCB%EXP_LEVEL_PER_COLI_BRAIN;
	 * @param expToSync l'expérience à ajouter
	 * @param cumulExpColiBrains l'expérience accumulée comptant pour les coliBrains
	 * @param usedColiBrains le nombre de coliBrains utilisés
	 */
	public void syncExpAndColiBrains(int expToSync, int cumulExpColiBrains, int usedColiBrains) {
		SQLiteDatabase database = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		JSONObject o = new JSONObject();
		try {
			o.put("task", "addExpAndCB");
			o.put("exp", expToSync);
			o.put("cumulExpCB", cumulExpColiBrains);
			o.put("usedCB", usedColiBrains);
			values.put("task", o.toString());
			database.insert("tasks", null, values);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		database.close();
	}
	
	/**
	 * Supprime tout avant une synchro totale.
	 */
	public void taskSyncTotale(int user) {
		SQLiteDatabase database = this.getWritableDatabase();
		database.delete("participations", null, null);
		database.delete("defis", null, null);
		database.delete("joueurs", "id<>"+user, null);
		database.close();
	}

	public AlertDialog.Builder execJSONTasks(Context context, JSONArray jsonArray, int user) {
		SQLiteDatabase database = this.getWritableDatabase();
		StringBuilder liste = new StringBuilder();
		for(int i=0; i<jsonArray.length(); i++) {
			try {
				JSONObject d = jsonArray.getJSONObject(i);
				String task = d.getString("task");
				if(task.equalsIgnoreCase("delPart")) {
					if(d.getInt("part_joueur")==user) { // Cas où la suppression vient du même joueur sur un autre appareil.
						database.delete("defis", "id="+d.getInt("part_defi"), null);
						database.delete("participations", "defi="+d.getInt("part_defi"), null);
					} else {
						database.delete("participations", "defi="+d.getInt("part_defi")+" AND joueur="+d.getInt("part_joueur"), null);
						liste.append(context.getResources().getString(R.string.deletedPart, d.getString("part_joueur_nom"), d.getString("part_defi_nom"))).append('\n');
					}
					cleanJoueurs(database, user);
				} else if(task.equalsIgnoreCase("delDefi")) {
					database.delete("defis", "id="+d.getInt("defi"), null);
					database.delete("participations", "defi="+d.getInt("defi"), null);
					cleanJoueurs(database, user);
					liste.append(context.getResources().getString(R.string.deletedDef, d.getString("defi_nom"))).append('\n');
				} else if(task.equalsIgnoreCase("newNiv")) {
					liste.append(context.getResources().getString(R.string.newNivDejaCree, d.getString("nomDefi"))).append('\n');
				} else if(task.equalsIgnoreCase("partObsolete")) {
					liste.append(context.getResources().getString(R.string.partObsolete, d.getString("nomDefi"))).append('\n');
				} else if(task.equalsIgnoreCase("triche")) {
					liste.append(context.getResources().getString(R.string.triche, d.getString("nomDefi"))).append('\n');
				} else if(task.equalsIgnoreCase("message")) {
					liste.append(' ').append(d.getString("message")).append('\n');
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		database.close();
		if(liste.length() > 0) {
			AlertDialog.Builder box = new AlertDialog.Builder(context);
			box.setTitle(R.string.notification);
			box.setMessage(liste);
			return box;
		}
		return null;
	}

}
