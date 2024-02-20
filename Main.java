package Main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Main
{
    static Connection Connex;   // unten initialisierbar wg. nötigem try-Block
    // Jeweils eigene (nicht-statische) Connection-Instanzen dann sinnvoll, wenn mehrere
    // unabhängige/parallele Zugriffe stattfinden sollen (s. Auskommentierungen unten)

    // Hilfsfunktionen für Ergebnis-Ausgabe (HD = true, wenn Header auszugeben)
    static List<String> ResultToList(ResultSet RS, boolean HD)
    {
        List<String> SL = new ArrayList<>();
        StringBuilder SB = new StringBuilder();

        try
        {
            ResultSetMetaData Meta = RS.getMetaData();
            int N = Meta.getColumnCount();

            // Headerzeile
            if (HD)
            {
                for (int I = 1; I <= N; I++)   // über alle Header-Spalten
                    SB.append(Meta.getColumnLabel(I).toUpperCase()).append("\t");
                SL.add(SB.toString());
            }

            // Datenzeilen
            while (RS.next())                   // über alle Daten-Zeilen
            {
                SB.setLength(0);
                for (int I = 1; I <= N; I++)    // über alle Daten-Spalten
                {
                    SB.append(RS.getObject(I));
                    if (I < N) SB.append('\t');
                }
                SL.add(SB.toString());
            }
        }
        catch (SQLException E)
        {
            System.out.println("Exception: " + E.getMessage());
        }
        return SL;
    }

    static String ResultToString(ResultSet RS)
    { return ResultToString(RS, false); }
    static String ResultToString(ResultSet RS, boolean HD)
    {
        List<String> RL = ResultToList(RS, HD);
        return String.join(System.lineSeparator(), RL);
    }

    // 0) Todo: Datenbank 'Comedies' selbst vorher via pgAdmin-Tool anlegen!

    // 1) Datenbank strukturieren (Datenbank-Modellierung)
    // * Normalisierte Datenbank (3NF = 3. Normalform):
    //   - Character: [CharID | Surname | Forename]
    //   - Show:      [Name | Country | Year]
    //   - PlaysIn:   [CharID | Name]
    //   - Actor:     [ActrID | Surname | Forename]
    //   - Performs:  [ActrID | CharID]
    // * Unnormalisierte Datenbank (1NF, 2NF)
    //   - 1NF: [CharID | C_Surname | C_Forename | A_Surname | A_Forename
    //           Showname | Country | Year] => unstrukturiert, redundant
    //   - 2NF: Würde z.B. dann vorliegen, wenn Show so konstruiert wäre:
    //          Show: [Name | Country | Year | Language]
    //          Language (der Show) wäre von Country abhängig, das selbst
    //          Schlüssel einer eigenen Tabelle [Country | Language] sein könnte

    /*
    Erste Normalform, Attribute einer Tabelle sind atmonar, es sollte keine wiederholten
        Gruppen geben.
    Zweite Normalform, wenn die in 1NF sind und jedes nicht Schlüsselelement voll funktional
        von jedem Kandidatenschlüssel abhängt. D.h. abhängigkeiten gibt es nur zum PS
    Dritte Normalform, wenn sie in 2NF ist und die Werte transitiv sind. D.h. Sie Nicht-Schlüssel
        Werte sollten nicht abhängig von Nicht-Schlüssel-Wertten sein. Country-Language is falsch
     */

    static void StructDB()
    {
        try                     // Achtung: muss immer in try-Block stehen
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            int Result;         // Wird hier nicht ausgewertet

            // ### Tabelle Character: [CharID | Surname | Forename] ###

            // Entfernbar, wenn vorhanden (sonst Exception)
            Result = Stmt.executeUpdate     // "CASCADE" bei Abhängigkeiten mit
                ("DROP TABLE IF EXISTS Character CASCADE"); // anderen Tabellen
            // Hinzufügbar, wenn noch nicht vorhanden (sonst Exception)
            Result = Stmt.executeUpdate
                ("""
                     CREATE TABLE Character
                     (CharID integer UNIQUE NOT NULL PRIMARY KEY,
                      Surname text NOT NULL,
                      Forename text NOT NULL)
                      """);     // statt 'text' auch 'char(N)' möglich

            // ### Tabelle Show: [Name | Country | Year] ###

            Result = Stmt.executeUpdate
                ("DROP TABLE IF EXISTS Show CASCADE");
            Result = Stmt.executeUpdate
                ("""
                     CREATE TABLE Show
                     (Name text UNIQUE NOT NULL PRIMARY KEY,
                      Country char(2) NOT NULL,
                      Year integer NOT NULL)
                     """);

            // ### Tabelle PlaysIn: [CharID | Name] ###

            Result = Stmt.executeUpdate
                ("DROP TABLE IF EXISTS PlaysIn");
            Result = Stmt.executeUpdate
                ("""
                     CREATE TABLE PlaysIn
                     (CharID integer NOT NULL REFERENCES Character,
                      Name text NOT NULL REFERENCES Show)
                      """);     // REFERENCES: Verweis auf Fremdschlüssel in anderen Tabellen

            // ### Tabelle Actor: [ActrID | Surname | Forename] ###

            Result = Stmt.executeUpdate
                ("DROP TABLE IF EXISTS Actor CASCADE");
            Result = Stmt.executeUpdate
                ("""
                     CREATE TABLE Actor
                     (ActrID char(3) UNIQUE NOT NULL PRIMARY KEY,
                      Surname text NOT NULL,
                      Forename text NOT NULL)
                      """);

            // ### Tabelle Performs: [ActrID | CharID] ###

            Result = Stmt.executeUpdate
                ("DROP TABLE IF EXISTS Performs");
            Result = Stmt.executeUpdate
                ("""
                     CREATE TABLE Performs
                     (ActrID char(3) NOT NULL REFERENCES Actor,
                      CharID integer NOT NULL REFERENCES Character)
                      """);     // REFERENCES: Verweis auf Fremdschlüssel in anderen Tabellen
                
            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }

    // 2) Daten eintragen (Datenbank-Update)
    // 2.1) Beispieldaten anlegen
    static void UpdateDB_1()
    {
        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            int Result;             // Update-Ergebnisse hier nicht ausgewertet

            // ### Character ###
            
            // Löschbar, falls vorhanden (Beispiele)
            Result = Stmt.executeUpdate
                ("DELETE FROM Character WHERE CharID=1");   // nur diesen Datensatz
            Result = Stmt.executeUpdate
                ("DELETE FROM Character");                  // alle Datensätze

            // Einfügbar, falls noch nicht vorhanden
            Result = Stmt.executeUpdate     // einzelnen Datensatz hinzufügen
                ("INSERT INTO Character VALUES(0, 'Simpson', 'Homer')");
            Result = Stmt.executeUpdate     // einzelnen Datensatz hinzufügen
                ("INSERT INTO Character VALUES(1, 'Simpson', 'Marge')");
            Stmt.addBatch("INSERT INTO Character VALUES(2, 'Simpson', 'Lisa')");
            Stmt.addBatch("INSERT INTO Character VALUES(3, 'Simpson', 'Bart')");
            Stmt.addBatch("INSERT INTO Character VALUES(4, 'Simpson', 'Maggie')");
            Stmt.executeBatch();            // mehrere Datensätze per Batch hinzufügen

            Result = Stmt.executeUpdate
                ("INSERT INTO Character VALUES(10, 'Seinfeld', 'Jerry')");
            Result = Stmt.executeUpdate
                ("INSERT INTO Character VALUES(11, 'Costanza', 'George')");
            Stmt.addBatch("INSERT INTO Character VALUES(12, 'Kramer', 'Cosmo')");
            Stmt.addBatch("INSERT INTO Character VALUES(13, 'Benes', 'Elaine')");
            Stmt.executeBatch();

            // ### Show ###
            
            Result = Stmt.executeUpdate             // bestimmte Show löschen
                ("DELETE FROM Show WHERE Name='The Simpsons'");
            Result = Stmt.executeUpdate
                ("DELETE FROM Show");           // alle Shows löschen

            Result = Stmt.executeUpdate
                ("INSERT INTO Show VALUES('The Simpsons', 'US', 1989)");
            Result = Stmt.executeUpdate
                ("INSERT INTO Show VALUES('Seinfeld', 'US', 1989)");

            // ### PlaysIn ###
            
            Result = Stmt.executeUpdate("DELETE FROM PlaysIn");

            Result = Stmt.executeUpdate
                ("INSERT INTO PlaysIn VALUES(0, 'The Simpsons')");
            Result = Stmt.executeUpdate
                ("INSERT INTO PlaysIn VALUES(1, 'The Simpsons')");
            Stmt.addBatch("INSERT INTO PlaysIn VALUES(2, 'The Simpsons')");
            Stmt.addBatch("INSERT INTO PlaysIn VALUES(3, 'The Simpsons')");
            Stmt.addBatch("INSERT INTO PlaysIn VALUES(4, 'The Simpsons')");
            Stmt.executeBatch();

            Result = Stmt.executeUpdate
                ("INSERT INTO PlaysIn VALUES(10, 'Seinfeld')");
            Result = Stmt.executeUpdate
                ("INSERT INTO PlaysIn VALUES(11, 'Seinfeld')");
            Stmt.addBatch("INSERT INTO PlaysIn VALUES(12, 'Seinfeld')");
            Stmt.addBatch("INSERT INTO PlaysIn VALUES(13, 'Seinfeld')");
            Stmt.executeBatch();

            // ### Actor ###
            
            Result = Stmt.executeUpdate("DELETE FROM Actor");

            Result = Stmt.executeUpdate
                ("INSERT INTO Actor VALUES('SJ1', 'Seinfeld', 'Jerry')");
            Stmt.addBatch("INSERT INTO Actor VALUES('AJ1', 'Alexander', 'Jason')");
            Stmt.addBatch("INSERT INTO Actor VALUES('RM1', 'Richards', 'Michael')");
            Stmt.addBatch("INSERT INTO Actor VALUES('LJ1', 'Louis-Dreyfus', 'Julia')");
            Stmt.executeBatch();

            // ### Performs ###
            
            Result = Stmt.executeUpdate("DELETE FROM Performs");

            Result = Stmt.executeUpdate
                ("INSERT INTO Performs VALUES('SJ1', 10)");
            Stmt.addBatch("INSERT INTO Performs VALUES('AJ1', 11)");
            Stmt.addBatch("INSERT INTO Performs VALUES('RM1', 12)");
            Stmt.addBatch("INSERT INTO Performs VALUES('LJ1', 13)");
            Stmt.executeBatch();

            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 2.2) Expliziter Daten-COMMIT bzw. -ROLLBACK
    static void UpdateDB_2()
    {
        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            Connex.setAutoCommit(false);      // Transaktionen müssen explizit abgeschlossen werden
            int Result;

            // ### COMMIT (Transaktion übernehmen) ###
            
            Result = Stmt.executeUpdate     // einzelnen Datensatz hinzufügen
                ("INSERT INTO Character VALUES(6, 'Simpson', 'Abraham')");
            Result = Stmt.executeUpdate
                ("DELETE FROM Character WHERE CharID=6");
            Connex.commit();                  // Transaktion wird bestätigt

            // ### ROLLBACK (Transaktion zurückrollen) ###
            try
            {
                Result = Stmt.executeUpdate
                    ("INSERT INTO Character VALUES(6, 'Simpson', 'Abraham')");
                Result = Stmt.executeUpdate     // doppelte CharID
                    ("INSERT INTO Character VALUES(6, 'Simpson', 'Mona')");
            }
            catch (SQLException E)
            { Connex.rollback(); }        // obige beide Update-Operationen zurücknehmen

            Connex.setAutoCommit(true);   // ab hier wieder impliziter/automatischer Transaktionsabschluss
            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }

    // 3) Daten abfragen (Datenbank-Recherche)
    // 3.1) Daten abfragen I (vollständig)
    static void QueryDB_1()
    {
        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;       // Subinterface RowSet mit zusätzlichen Methoden

            // ### Character ###
            
            System.out.println("\r\n=== Character-Daten ===");
            RS = Stmt.executeQuery("SELECT * FROM Character");
            System.out.println(ResultToString(RS));
            RS.close();

            // ### Show ###
            
            System.out.println("\r\n=== Show-Daten ===");
            RS = Stmt.executeQuery("SELECT * FROM Show");
            System.out.println(ResultToString(RS));
            RS.close();

            // ### PlaysIn ###
            
            System.out.println("\r\n=== PlaysIn-Daten ===");
            RS = Stmt.executeQuery("SELECT * FROM PlaysIn");
            System.out.println(ResultToString(RS));
            RS.close();

            // ### Actor ###
            
            System.out.println("\r\n=== Actor-Daten ===");
            RS = Stmt.executeQuery("SELECT * FROM Actor");
            System.out.println(ResultToString(RS));
            RS.close();

            // ### Performs ###
            
            System.out.println("\r\n=== Performs-Daten ===");
            RS = Stmt.executeQuery("SELECT * FROM Performs");
            System.out.println(ResultToString(RS));
            RS.close();

            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 3.2) Daten abfragen II (spalten-/auszugsweise, bedingt/sortiert)
    // Selektion (Zeilenextraktion mittels WHERE), Projektion (Spaltenextraktion)
    static void QueryDB_2()
    {
        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;

            // ### Show ###
            
            System.out.println("\r\n=== Partial Show Data ===");
            RS = Stmt.executeQuery("SELECT Name FROM Show");    // Projektion: Spalte selektieren
            System.out.println(ResultToString(RS));
            RS.close();

            // ### Actor ###
            
            System.out.println("\r\n=== Partial Actor Data ===");
            RS = Stmt.executeQuery("SELECT Forename, Surname FROM Actor");
            System.out.println(ResultToString(RS));
            RS.close();

            // ### Character ###
            
            System.out.println("\r\n=== Partial Character Data ===");
            RS = Stmt.executeQuery("""
                                       SELECT Forename, Surname FROM Character
                                       WHERE Surname='Simpson'
                                       ORDER BY Forename, Surname
                                       """);        // Selektion durch WHERE (Zeile) und
            System.out.println(ResultToString(RS)); // Projektion durch SELECT (Spalte) (sic!)
            RS.close();

            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 3.3) Daten abfragen III (tabellenübergreifend/verknüpfend)
    static void QueryDB_3()
    {
        // Jedes SELECT erzeugt eine Menge (aus Zeilen von Spalten),
        // das mit den Mengenoperatoren verknüpft werden kann

        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;

            // ### INTERSECTION ###
            
            System.out.println("\r\n=== INTERSECTION ===");
            RS = Stmt.executeQuery("""
                                       SELECT Surname, Forename FROM Character INTERSECT
                                       SELECT Surname, Forename FROM Actor
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();

            // ### UNION ###
            
            System.out.println("\r\n=== UNION ===");
            RS = Stmt.executeQuery("""
                                       SELECT Surname, Forename FROM Character UNION
                                       SELECT Surname, Forename FROM Actor
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();

            // ### DIFFERENCE ###
            
            System.out.println("\r\n=== DIFFERENCE ===");
            RS = Stmt.executeQuery("""
                                       SELECT Surname, Forename FROM Character EXCEPT
                                       SELECT Surname, Forename FROM Actor
                                       """);    // EXCEPT wie SUBTRACTION
            System.out.println(ResultToString(RS));
            RS.close();

            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 3.4) Daten abfragen IV (tabellenübergreifend/verknüpfend)
    static void QueryDB_4()
    {
        // Das Kartesische Produkt erzeugt alle Kombinationen jeder Zeile
        // einer Tabelle T1 mit jeder Zeile einer Tabelle T2, so dass
        // jede Ergebnis-/Produktzeile aus allen Spalten der gerade
        // kombinierten Zeilen von T1 und T2 besteht

        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;

            // ### CARTESIAN PRODUCT ###
            
            System.out.println("\r\n=== CARTESIAN PRODUCT ===");
            RS = Stmt.executeQuery("""
                                       SELECT Character.*, Actor.* FROM Character, Actor
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();

            // ### JOIN ###
            
            System.out.println("\r\n=== EQUI-JOIN ===");
            RS = Stmt.executeQuery("""
                                       SELECT Character.*, Actor.* FROM Character, Actor
                                       WHERE Character.Surname = Actor.Surname AND
                                             Character.Forename = Actor.Forename
                                       """);        // nur Nach-/Vorname ausgegeben
            /*RS = Stmt.executeQuery("""
                                   SELECT * FROM Character, Actor
                                   WHERE Character.Surname = Actor.Surname AND
                                         Character.Forename = Actor.Forename
                                   """);        // nur Nach-/Vorname ausgegeben*/
            /*RS = Stmt.executeQuery("""
                                   SELECT * FROM Character INNER JOIN Actor
                                   ON Character.Surname = Actor.Surname AND
                                      Character.Forename = Actor.Forename
                                   """);        // nur Nach-/Vorname ausgegeben*/
            System.out.println(ResultToString(RS));
            RS.close();

            // ### NATURAL JOIN ###
            
            System.out.println("\r\n=== NATURAL JOIN ===");
            RS = Stmt.executeQuery("""
                                       SELECT * FROM Actor NATURAL JOIN Performs
                                       """);        // Zeilen mit gleichen ActrIDs selektiert
            System.out.println(ResultToString(RS)); // und CharID hinzugenommen (Kartes. Produkt)
            RS.close();

            // ### SEMI-JOIN ###
            
            System.out.println("\r\n=== SEMI-JOIN ===");
            RS = Stmt.executeQuery("""
                                       SELECT ActrID, Surname, Forename FROM Actor NATURAL JOIN Performs
                                       """);        // wie NATURAL JOIN, aber nur linke Tabelle
            /*RS = Stmt.executeQuery("""
                                   SELECT Actor.ActrID, Actor.Surname, Actor.Forename
                                   FROM Actor INNER JOIN Performs
                                   ON Actor.ActrID = Performs.ActrID
                                   """);            // wie NATURAL JOIN, aber nur linke Tabelle*/
            System.out.println(ResultToString(RS));
            RS.close();

            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 3.5) Daten abfragen V (tabellenübergreifend/verknüpfend)
    static void QueryDB_5()
    {
        // S. auch Beispiele unter
        // https://de.wikibooks.org/wiki/Relationenalgebra_und_SQL:_Outer-Join

        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;

            // ### FULL OUTER JOIN (alle Schlüssel beider Tabellen kombiniert ) ###
            
            System.out.println("\r\n=== FULL OUTER JOIN ===");
            RS = Stmt.executeQuery("""
                                   SELECT * FROM Performs FULL OUTER JOIN Character
                                            USING (CharID)
                                   """);  // andere Spaltenfolge als unten
            // CharID für beide gemeinsam: nur einmal ausgegeben
            // Nicht jeder Charakter wird durch Akteur verkörpert => nulls
            /*RS = Stmt.executeQuery("""
                                       SELECT * FROM Character FULL OUTER JOIN Performs
                                                USING (CharID)
                                       """);    // zufällig gleich mit NATURAL LEFT OUTER JOIN */
            System.out.println(ResultToString(RS));
            RS.close();

            // ### NATURAL LEFT OUTER JOIN (nur die Schlüssel der linken Tabelle) ###
            
            System.out.println("\r\n=== NATURAL LEFT OUTER JOIN ===");
            RS = Stmt.executeQuery("""
                                       SELECT * FROM Character NATURAL LEFT OUTER JOIN Performs
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();

            // ### LEFT OUTER JOIN (nur die Schlüssel der linken Tabelle) ###
            
            System.out.println("\r\n=== LEFT OUTER JOIN ===");
            RS = Stmt.executeQuery("""
                                       SELECT * FROM Character LEFT OUTER JOIN Performs
                                                ON Character.CharID = Performs.CharID
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();

            // ### NATURAL RIGHT OUTER JOIN (nur die Schlüssel der rechten Tabelle) ###
            
            System.out.println("\r\n=== NATURAL RIGHT OUTER JOIN ===");
            RS = Stmt.executeQuery("""
                                       SELECT * FROM Character NATURAL RIGHT OUTER JOIN Performs
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();

            // ### RIGHT OUTER JOIN (nur die Schlüssel der rechten Tabelle) ###
            
            System.out.println("\r\n=== RIGHT OUTER JOIN ===");
            RS = Stmt.executeQuery("""
                                       SELECT * FROM Character RIGHT OUTER JOIN Performs
                                                ON Character.CharID = Performs.CharID
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();

            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 3.6) Daten abfragen VI (verschachtelnd)
    static void QueryDB_6()
    {
        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;
            int Result;

            // ### SELECT from SELECT (Subtable with Alias) ###
            
            // (Aus Charaktere alle auswählen, die auch Akteur haben;
            //  daraus alle Charaktere auswählen, deren Vorname mit 'J' beginnt)
            System.out.println("\r\n=== SELECT FROM SELECT 1 ===");
            RS = Stmt.executeQuery("""
                                       SELECT Forename FROM
                                              (SELECT Character.Surname,
                                                      Character.Forename,
                                                      Character.CharID,
                                                      Performs.CharID
                                                 FROM Character, Performs
                                               WHERE Character.CharID =
                                                     Performs.CharID
                                              ) AS Subtable
                                       WHERE Subtable.Forename LIKE 'J%'
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();

            // ### SELECT from SELECT (Subtable with Alias) ###
            
            // (Alle Shows sortiert, in denen Charaktere auftreten,
            //  wo Nachname auch in Show-Titel vorkommt)
            System.out.println("\r\n=== SELECT FROM SELECT 2a ===");
            RS = Stmt.executeQuery("""
                                       SELECT DISTINCT Name FROM
                                              (SELECT Character.Surname, Show.Name
                                                 FROM Character, Show
                                               WHERE POSITION(Character.Surname IN
                                                              Show.Name) > 0
                                              ) AS Subtable
                                       ORDER BY Subtable.Name
                                       """);    // (ohne neue Daten kommen beide Shows raus)
            System.out.println(ResultToString(RS));
            RS.close();

            // ### SELECT from SELECT (Subtable without Alias) ###
            
            // (Alle Shows sortiert, in denen Charaktere auftreten,
            //  wo Nachname auch in Show-Titel vorkommt)
            System.out.println("\r\n=== SELECT FROM SELECT 2b ===");
            RS = Stmt.executeQuery("""
                                       SELECT Name FROM Show
                                       WHERE EXISTS
                                             (SELECT Character.Surname, Show.Name
                                                FROM Character, Show
                                              WHERE POSITION(Character.Surname IN
                                                             Show.Name) > 0
                                             )
                                       """);    // (kein Alias nötig, da keine Sortierung)
            System.out.println(ResultToString(RS));
            RS.close();

            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 3.7) Daten abfragen VII (spaltenweise Aggregation)
    static void QueryDB_7()
    {
        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;

            // ### AGGREGATION: MIN/MAX ###
            
            System.out.println("\r\n=== MAX/MIN ===");
            RS = Stmt.executeQuery("SELECT MAX(Surname), MIN(Forename) FROM Character");
            System.out.println(ResultToString(RS));
            RS.close();

            // ### AGGREGATION: MAX/MIN ###
            
            System.out.println("\r\n=== MIN/MAX ===");
            RS = Stmt.executeQuery("SELECT MIN(Surname), MAX(Forename) FROM Character");
            System.out.println(ResultToString(RS));
            RS.close();

            // ### AGGREGATION: COUNT ###
            
            System.out.println("\r\n=== COUNT ===");
            RS = Stmt.executeQuery("SELECT COUNT(Surname) FROM Character");
            System.out.println(ResultToString(RS));
            RS.close();

            // ### AGGREGATION: SUM ###
            
            System.out.println("\r\n=== SUM ===");
            RS = Stmt.executeQuery("SELECT SUM(Year) FROM Show");
            System.out.println(ResultToString(RS));
            RS.close();

            // ### AGGREGATION: AVG ###
            
            System.out.println("\r\n=== AVERAGE ===");
            RS = Stmt.executeQuery("SELECT AVG(Year) FROM Show");
            System.out.println(ResultToString(RS));
            RS.close();
            
            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 3.8) Daten abfragen VIII (spaltenweise Aggregation und Gruppierung)
    static void QueryDB_8()
    {
        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;
            
            // ### WITHOUT GROUPING ###
            
            System.out.println("\r\n=== NO GROUPING I ===");
            RS = Stmt.executeQuery("SELECT Surname FROM Character");
            System.out.println(ResultToString(RS));
            RS.close();
            
            System.out.println("\r\n=== NO GROUPING II ===");
            RS = Stmt.executeQuery("SELECT COUNT(Surname) FROM Character");
            System.out.println(ResultToString(RS));
            RS.close();
            
            // ### WITH GROUPING ###
            
            System.out.println("\r\n=== GROUPING ===");
            RS = Stmt.executeQuery("""
                                       SELECT Surname, COUNT(Surname)
                                       FROM Character
                                       GROUP BY Surname
                                       """);        // GROUP BY zus. mit Aggregat-Funktion
            System.out.println(ResultToString(RS)); // (Zählung innerhalb Nachnamen-Gruppen)
            RS.close();
            
            // ### WITH GROUPING/HAVING ###
            
            System.out.println("\r\n=== GROUPING/HAVING I ===");
            RS = Stmt.executeQuery("""
                                       SELECT Surname, COUNT(Surname)
                                       FROM Character
                                       GROUP BY Surname
                                       HAVING CHAR_LENGTH(Surname) >= 8
                                       """);    // HAVING zus. mit GROUP BY (= WHERE für GROUPING)
            System.out.println(ResultToString(RS));
            RS.close();
    
            System.out.println("\r\n=== GROUPING/HAVING II ===");
            RS = Stmt.executeQuery("""
                                       SELECT Surname, COUNT(Surname)
                                       FROM Character
                                       GROUP BY Surname
                                       HAVING Surname LIKE 'S%'
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();
    
            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 3.9) Daten abfragen IX (Views als virtuelle Tabellen)
    static void QueryDB_9()
    {
        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;
            int Result;     // kann ausgewertet werden

            // ### VIEW ###
            
            System.out.println("\r\n=== VIEW ===");
            Result = Stmt.executeUpdate("""
                                            CREATE VIEW Show80s AS
                                            SELECT * FROM Show
                                            WHERE Year >= 1980 AND
                                                  Year <= 1989
                                            """);
            RS = Stmt.executeQuery("""
                                       SELECT * FROM Show80s
                                       WHERE Name LIKE 'S%'
                                       """);
            System.out.println(ResultToString(RS));
            RS.close();

            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
    // 3.10) Daten verarbeiten (Verschiedenes)
    static void QueryDB_10()
    {
        try
        {
            //Connection Connex = DriverManager.getConnection("jdbc:postgresql:Comedies", "postgres", "blubb");
            Statement Stmt = Connex.createStatement();
            ResultSet RS;

            // Umbenennung Spalte für Ausgabe
            System.out.println("\r\n=== Renaming of column ===");
            RS = Stmt.executeQuery      // Alias-Spaltenname
                         ("""
                          SELECT Surname, CharID AS CharacterID FROM Character
                          WHERE CharID < 10
                          """);
            System.out.println(ResultToString(RS, true)+"\r\n");
            RS = Stmt.executeQuery      // Original-Spaltenname
                ("""
                          SELECT Surname, CharID FROM Character
                          WHERE CharID < 10
                          """);
            System.out.println(ResultToString(RS, true));
            RS.close();

            // Typ-Umwandlung
            System.out.println("\r\n=== Typecast ===");
            RS = Stmt.executeQuery
                         ("""
                          SELECT CAST(Year AS REAL) FROM Show
                          """);
            System.out.println(ResultToString(RS));
            RS.close();

            // Duplikate entfernen
            System.out.println("\r\n=== Duplicate removal ===");
            RS = Stmt.executeQuery  // s.auch Streams bzgl. 'distinct'
                         ("""
                          SELECT DISTINCT Year FROM Show
                          """);
            System.out.println(ResultToString(RS));
            RS.close();

            // Berechnung innerhalb Anfrage
            System.out.println("\r\n=== Calculation within query ===");
            RS = Stmt.executeQuery
                         ("""
                          SELECT MAX(Year)-MIN(Year) AS Range FROM Show
                          """);
            System.out.println(ResultToString(RS));
            RS.close();

            // Vergleich mit Menge von Elementen
            System.out.println("\r\n=== Comparation with set of values ===");
            RS = Stmt.executeQuery          // mit BETWEEN sinnvoller
                         ("""
                          SELECT CharID FROM Character
                          WHERE CharID IN (0, 1, 2, 3, 4)
                          """);
            System.out.println(ResultToString(RS));
            RS.close();

            // Bedingtes Ergebnis
            System.out.println("\r\n=== Conditioned result (find) ===");
            RS = Stmt.executeQuery      // beide Shows als Ergebnis (eingebettes SELECT findet nur 'Seinfeld')
                         ("""
                          SELECT Name FROM Show
                          WHERE EXISTS
                                (SELECT Actor.ActrID FROM Actor, Performs
                                 WHERE Actor.ActrID = Performs.ActrID)
                          """);
            System.out.println(ResultToString(RS));
            System.out.println("\r\n=== Conditioned result (non-find) ===");
            RS = Stmt.executeQuery      // keine Show als Ergebnis
                         ("""
                          SELECT CharID FROM Character
                          WHERE EXISTS
                                (SELECT Year FROM Show
                                 WHERE Year >= 1990)
                          """);
            System.out.println(ResultToString(RS));
            RS.close();

            Stmt.close();
            //Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }

    // Todo Fragen:
    // * Beschreiben Sie den Unterschied zwischen den Tabellentypen
    //   Character/Actor/Show vs. PlaysIn/Performs.
    //   Was entspricht wem in der natürlichen Sprache (Beispiel):
    //   "Mann liebt Frau" vs. "Vater von Eva liebt Mutter von Eva"
    // * Was müsste man wo ändern, wenn es zufällig zwei Shows gleichen
    //   Namens gäbe?
    // * Dürfen 'Foreign Keys' nur auf 'Primary Keys' referenzieren oder
    //   auch auf andere 'Foreign Keys'? (s. StructDB für Beispiel)
    // * Normalisierung: Tabelle [PLZ_ID PLZ Ort] sinnvoll?
    // Todo Aufgaben:
    // * Daten aus Character (oder Actor) mit Vor- und Nachnamen als
    //   Ausgabe/-Ergebnistabelle (statt standardmäßig Nach- und Vorname)
    // * Daten, wo Character und Actor gleichen Nachnamen aufweisen
    // * Daten aus Character (oder Actor), deren Vornamen mit 'J' beginnen
    // * Daten aus Character (oder Actor), deren Vor- und Nachnamen gleich
    //   viele Zeichen aufweisen bzw. sich max. um 1 Zeichen unterscheiden
    // * Daten aus Character (oder Actor), deren Nachname im Shownamen auftritt
    // * Daten aus Character (oder Actor), deren Vorname (oder Nachname) mit denselben
    //   ein oder zwei Zeichen beginnt und endet (z.B. "Elaine" [E/e], "George" [Ge/ge])
    // * Daten aus Character, die durch keinen Akteur verkörpert werden
    //   (fiktionale Charaktere)
    // * Shows mit ihren Akteuren (ActrID genügt)
    // * Shows, die mind. 4 (bzw. 5) Characters (Mitspieler) haben
    // * Shows mit Namen, die gleich mit Character- und Actor-Nachnamen sind
    // * Ermittlung der durchschnittlichen Anzahl Characters (oder Actors) pro Show

    public static void main(String[] args)
    {
        // Hinweis: JDBC-Treiber für postgreSQL muss als JAR eingebunden werden
        //          via [File] -> [Project Structure] -> [Libraries]
        // Adresse: https://jdbc.postgresql.org/download.html

        try
        { Class.forName("org.postgresql.Driver"); }
        catch (ClassNotFoundException E)
        { System.out.println("Exception: " + E.getMessage()); }

        try
        {
            // Todo: Hier evtl. eigenes Passwort einfügen (statt "blubb")!
            Connex = DriverManager.getConnection  // wenn erste PG-Installation
                ("jdbc:postgresql:Comedies", "postgres", "blubb");
            /*Connex = DriverManager.getConnection    // nur wenn weitere PG-Updates
                ("jdbc:postgresql://localhost:5433/Comedies", "postgres", "blubb");*/

            StructDB();

            UpdateDB_1();
            //UpdateDB_2();

            //QueryDB_1();
            //QueryDB_2();
            //QueryDB_3();
            //QueryDB_4();
            //QueryDB_5();
            //QueryDB_6();
            //QueryDB_7();
            //QueryDB_8();
            //QueryDB_9();
            //QueryDB_10();

            Connex.close();
        }
        catch (SQLException E)
        { System.out.println("Exception: " + E.getMessage()); }
    }
}
