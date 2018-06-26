package it.polimi.ingsw.view.cli;

import it.polimi.ingsw.controller.RemoteGameController;
import it.polimi.ingsw.model.GameModel;
import it.polimi.ingsw.model.Player;
import it.polimi.ingsw.model.RemoteGameModel;
import it.polimi.ingsw.model.States;
import it.polimi.ingsw.view.RemoteView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;

public class ViewCLI extends UnicastRemoteObject implements RemoteView, Serializable {

    private States state;
    private String user;
    private transient Scanner input;
    private boolean endGame;
    private int choose1;
    private int choose2;
    private ArrayList<Integer> choices;
    private boolean online;
    private RemoteGameModel gameModel;
    private RemoteGameController network;

    private boolean returnOnline;

    private boolean socketConnection;

    private transient Socket socket;

    private boolean startTimerSocket;

    private boolean deleteConnectionSocket;

    /**
     * creates a ViewCLI object checking if the username is correct and if the game is already started
     * initializes an arraylist of integer which will contains client's inputs
     * @param network the RemoteGameController
     * @throws RemoteException if the reference could not be accessed
     */
    public ViewCLI(RemoteGameController network, boolean socketConnection, Socket socket) throws IOException, ClassNotFoundException {
        System.out.println("WELCOME TO SAGRADA! \n\n\n");
        this.socketConnection = socketConnection;
        this.socket = socket;
        choices = new ArrayList<>();
        returnOnline = false;
        startTimerSocket = false;
        deleteConnectionSocket = false;
        setOnline(true);
        endGame = false;
        if (this.socketConnection) {
            setUser();
            ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
            ob.writeObject(this);
            updateSocket();
        } else {
            this.network = network;
            if (network.getMultiPlayerStarted()) {
                gameModel = network.getGameModel();
                if (gameModel.getState().equals(States.LOBBY)) {
                    do {
                        setUser();
                    } while (!verifyUser(user));
                    network.addObserver(this);
                    network.update(this);
                } else {
                    if (!gameModel.getObservers().contains(null)) {
                        System.out.println("OPS! THE GAME IS ALREADY STARTED!\n\nCOME BACK LATER!");
                        System.exit(0);
                    } else {
                        do {
                            setUser();
                        } while (!verifyUserCrashed(user));
                        network.reAddObserver(this);
                        network.setPlayerOnline(user, true);
                        System.out.println("\n\nJOINING AGAIN THE MATCH...");
                    }
                }
            } else if (!network.getSinglePlayerStarted()) {
                network.createGameModel( 0);
                gameModel = network.getGameModel();
                if (gameModel.getPlayers().isEmpty())
                    setUser();
                else {
                    do {
                        setUser();
                    } while (!verifyUser(user));
                }
                network.addObserver(this);
                network.update(this);
            } else {
                System.out.println("OPS! THE GAME IS ALREADY STARTED!\n\nCOME BACK LATER!");
                System.exit(0);
            }
        }
    }

    public void socketTimeOut(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ObjectInputStream ob = new ObjectInputStream(socket.getInputStream());
                    gameModel = (RemoteGameModel) ob.readObject();
                    setDeleteConnectionSocket(false);
                    for(Player x : gameModel.getPlayers()){
                        if(x.getUsername().equals(user) && !x.getOnline())
                            setOnline(false);
                    }
                }catch (IOException e){
                    //
                }catch (ClassNotFoundException e1){
                    //
                }
            }
        }).start();
    }

    /**
     * asks the client to enter an username
     * finally transform it in upper case
     */
    private void setUser() {
        input = new Scanner(System.in);
        System.out.println("ENTER YOUR USERNAME:");
        this.user = input.next().toUpperCase();
    }

    @Override
    public synchronized boolean getDeleteConnectionSocket() {
        return deleteConnectionSocket;
    }

    public synchronized void setDeleteConnectionSocket(boolean x){
        this.deleteConnectionSocket = x;
    }

    @Override
    public boolean getReturnOnline(){
        return returnOnline;
    }

    @Override
    public boolean getStartTimerSocket() {
        return startTimerSocket;
    }

    /**
     * verifies if the client has entered a not valid username
     * @param s the name of the client to be verified
     * @return true if the username is valid, false otherwise
     * @throws RemoteException if the reference could not be accessed
     */
    private boolean verifyUser(String s) throws RemoteException{
        for(int i=0; i<gameModel.getPlayers().size(); i++){
            if(s.equals(gameModel.getPlayers().get(i).getUsername())){
                System.out.println("THIS USERNAME ALREADY EXISTS");
                return false;
            }
        }
        return true;
    }

    /**
     * verifies if some client has lost connection to the main server
     * @param s the name of the client to be verified
     * @return true if the client has lost connection, false otherwise
     * @throws RemoteException if the reference could not be accessed
     */
    private boolean verifyUserCrashed(String s) throws RemoteException {
        for(Player x : gameModel.getPlayers()){
            if(x.getUsername().equals(s)){
                if(x.getOnline())
                    return false;
                else{
                    for(RemoteView y : gameModel.getObservers()){
                        if(y!=null && y.getUser().equals(s))
                            return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * sets if the client is online or not
     * @param online the boolean to be set
     */
    @Override
    public synchronized void setOnline(boolean online){
        this.online = online;
        if(!online){
            this.print("\n\nYOU ARE NOW INACTIVE! TO JOIN AGAIN THE MATCH, PLEASE PRESS 0");
        }
    }

    /**
     * gets if player is online or not
     * @return true if the player is online, false otherwise
     */
    @Override
    public synchronized boolean getOnline(){
        return online;
    }

    /**
     * gets the client's username
     * @return the client's username
     */
    @Override
    public String getUser() {
        return user;
    }

    /**
     * gets choose1
     * @return first choice of the client
     */
    @Override
    public int getChoose1() {
        return choose1;
    }

    /**
     * gets choose2
     * @return second choice of the client
     */
    @Override
    public int getChoose2() {
        return choose2;
    }

    /**
     * gets if the game is ended or not
     * @return true if game is ended, false otherwise
     */
    @Override
    public boolean getEndGame() {
        return endGame;
    }

    /**
     * gets the list of inputs of the client
     * @return an arraylist of client's inputs
     */
    @Override
    public ArrayList<Integer> getChoices(){
        return choices;
    }

    /**
     * sets choose1
     * @param choose1 the int to be set
     */
    private void setChoose1(int choose1){
        this.choose1 = choose1;
    }

    /**
     * sets choose2
     * @param choose2 the int to be set
     */
    private void setChoose2(int choose2){
        this.choose2 = choose2;
    }

    /**
     * chooses the next state according to the actual one
     * @throws RemoteException if the reference could not be accessed
     */
    private void run() throws IOException {

        returnOnline = false;
        state = gameModel.getState();

        switch (state){
            case LOBBY:
                viewLobby();
                break;
            case ENDROUND:
                viewEndRound();
                break;
            case ENDMATCH:
                viewEndMatch();
                break;
            case SELECTWINDOW:
                viewSelectWindow();
                break;
            case SELECTMOVE1:
                viewSelectMove1();
                break;
            case SELECTMOVE2:
                viewSelectMove2();
                break;
            case PUTDICEINWINDOW:
                viewPutDiceInWindow();
                break;
            case SELECTDRAFT:
                viewSelectDraft();
                break;
            case SELECTCARD:
                viewSelectCard();
                break;
            case USETOOLCARD:
                viewUseToolCard();
                break;
            case USETOOLCARD2:
                viewUseToolCard2();
                break;
            case USETOOLCARD3:
                viewUseToolCard3();
                break;
            case ERROR:
                viewError();
                break;
            default:
                assert false;
        }
    }

    /**
     * prints the players in the lobby
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewLobby() throws RemoteException {
        System.out.println("GAMERS IN THE LOBBY:");
        for(Player x: gameModel.getPlayers()){
            System.out.println("- "+ x.getUsername());
        }

    }

    /**
     * prints a message for each player to notify them the end of a round
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewEndRound() throws IOException {
        System.out.println("\n\nEND OF ROUND " + gameModel.getField().getRoundTrack().getRound() +"\n\n");
        if(user.equals(gameModel.getActualPlayer().getUsername())){
            if(socketConnection){
                ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                ob.writeObject(this);
            }
            else
                network.update(this);
        }
    }

    /**
     * prints the final score for each player
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewEndMatch() throws RemoteException {
        boolean win = true;
        int myScore = 0;
        for(Player x : gameModel.getPlayers()){
            if(user.equals(x.getUsername())){
                myScore = x.getFinalScore();
                System.out.println("YOUR FINAL SCORE IS: "+ myScore +"\n");
                break;
            }
        }
        for (Player x : gameModel.getPlayers()){
            if(!user.equals(x.getUsername()))
                System.out.println(x.getUsername() +"'S FINAL SCORE: "+ x.getFinalScore());
            if(myScore < x.getFinalScore())
                win = false;
        }
        if (win)
            System.out.println("\nYOU WON!!!");
        else
            System.out.println("\nYOU LOST...    :'(");
    }

    /**
     * prints the 2 schemecards (4 window)
     * @throws RemoteException if the reference could not be accessed
     */
    //The selectwindow is without timer
    private void viewSelectWindow() throws IOException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            System.out.println("SELECT YOUR WINDOW!");
            PrintSchemeCard.print(gameModel.getSchemeCards().get(0), gameModel.getSchemeCards().get(1));
            input = new Scanner(System.in);
            while(!input.hasNextInt())
                input = new Scanner(System.in);
            setChoose1(input.nextInt());
            if(socketConnection){
                ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                ob.writeObject(this);
            }
            else
                network.update(this);
        }
        else{
            System.out.println("\n\nWAIT YOUR TURN...\n\n");
        }
    }

    /**
     * prints client's input possible choices
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewSelectMove1() throws IOException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            if(socketConnection){
                startTimerSocket = true;
                ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                ob.writeObject(this);
                socketTimeOut();
            }
            else
                network.startTimer(this, null);
            startTimerSocket = false;
            int tmp = ShowGameStuff.print((GameModel) gameModel, false);
            while (tmp != 0) {
                tmp = ShowGameStuff.print((GameModel) gameModel, false);
            }
            if(getOnline()) {
                PrintSelectMove1.print();
                input = new Scanner(System.in);
                while(!input.hasNextInt())
                    input = new Scanner(System.in);
                setChoose1(input.nextInt());
                if (getOnline()) {
                    if(socketConnection){
                        setDeleteConnectionSocket(true);
                        ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                        ob.writeObject(this);
                    }
                    else
                        network.update(this);
                } else {
                    if(socketConnection){
                        returnOnline = true;
                        ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                        ob.writeObject(this);
                        this.setOnline(true);
                    }
                    else {
                        network.setPlayerOnline(user, true);
                        this.setOnline(true);
                    }
                }
            }
            else{
                if(socketConnection){
                    returnOnline = true;
                    ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                    ob.writeObject(this);
                    this.setOnline(true);
                }
                else {
                    network.setPlayerOnline(user, true);
                    this.setOnline(true);
                }
            }
        }
        else{
            System.out.println("\n\nWAIT YOUR TURN...\n\n");
        }
    }

    /**
     * prints client's input possible choices
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewSelectMove2() throws IOException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            if(socketConnection)
                socketTimeOut();
            int tmp = ShowGameStuff.print((GameModel) gameModel, false);
            while (tmp != 0) {
                tmp = ShowGameStuff.print((GameModel) gameModel, false);
            }
            if(getOnline()) {
                PrintSelectMove2.print();
                input = new Scanner(System.in);
                while(!input.hasNextInt())
                    input = new Scanner(System.in);
                setChoose1(input.nextInt());
                if (getOnline()) {
                    if(socketConnection){
                        setDeleteConnectionSocket(true);
                        ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                        ob.writeObject(this);
                    }
                    else
                        network.update(this);
                } else {
                    if(socketConnection){
                        returnOnline = true;
                        ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                        ob.writeObject(this);
                        this.setOnline(true);
                    }
                    else {
                        network.setPlayerOnline(user, true);
                        this.setOnline(true);
                    }
                }
            }
            else{
                if(socketConnection){
                    returnOnline = true;
                    ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                    ob.writeObject(this);
                    this.setOnline(true);
                }
                else {
                    network.setPlayerOnline(user, true);
                    this.setOnline(true);
                }
            }
        }
    }

    /**
     * prints the player's window and asks him the i,j position to insert it
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewPutDiceInWindow() throws IOException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            if(socketConnection)
                socketTimeOut();
            PrintWindow.print(gameModel.getActualPlayer().getWindow());
            System.out.println("CHOOSE A ROW TO PUT YOUR DICE (-1 TO ABORT)");
            input = new Scanner(System.in);
            while(!input.hasNextInt())
                input = new Scanner(System.in);
            setChoose1(input.nextInt());
            if(getOnline()) {
                if (choose1 != -1) {
                    System.out.println("CHOOSE A COLUMN TO PUT YOUR DICE (-1 TO ABORT)");
                    input = new Scanner(System.in);
                    while (!input.hasNextInt())
                        input = new Scanner(System.in);
                    setChoose2(input.nextInt());
                }
                if (getOnline()) {
                    if(socketConnection){
                        setDeleteConnectionSocket(true);
                        ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                        ob.writeObject(this);
                    }
                    else
                        network.update(this);
                } else {
                    if(socketConnection){
                        returnOnline = true;
                        ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                        ob.writeObject(this);
                        this.setOnline(true);
                    }
                    else {
                        network.setPlayerOnline(user, true);
                        this.setOnline(true);
                    }
                }
            }
            else{
                if(socketConnection){
                    returnOnline = true;
                    ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                    ob.writeObject(this);
                    this.setOnline(true);
                }
                else {
                    network.setPlayerOnline(user, true);
                    this.setOnline(true);
                }
            }
        }
    }

    /**
     * prints the list of dice in the draft
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewSelectDraft() throws IOException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            if(socketConnection)
                socketTimeOut();
            System.out.println("SELECT A DICE (-1 TO ABORT)");
            PrintDraft.print(gameModel.getField().getDraft());
            input = new Scanner(System.in);
            while(!input.hasNextInt())
                input = new Scanner(System.in);
            setChoose1(input.nextInt());
            if (getOnline()) {
                if(socketConnection){
                    setDeleteConnectionSocket(true);
                    ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                    ob.writeObject(this);
                }
                else
                    network.update(this);
            } else {
                if(socketConnection){
                    returnOnline = true;
                    ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                    ob.writeObject(this);
                    this.setOnline(true);
                }
                else {
                    network.setPlayerOnline(user, true);
                    this.setOnline(true);
                }
            }
        }
    }

    /**
     * prints toolcards available
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewSelectCard() throws IOException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            if(socketConnection)
                socketTimeOut();
            System.out.println("SELECT A TOOLCARD (-1 TO ABORT)");
            PrintToolCard.print(gameModel.getField().getToolCards());
            input = new Scanner(System.in);
            while(!input.hasNextInt())
                input = new Scanner(System.in);
            setChoose1(input.nextInt());
            if (getOnline()) {
                if(socketConnection){
                    setDeleteConnectionSocket(true);
                    ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                    ob.writeObject(this);
                }
                else
                    network.update(this);
            } else {
                if(socketConnection){
                    returnOnline = true;
                    ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                    ob.writeObject(this);
                    this.setOnline(true);
                }
                else {
                    network.setPlayerOnline(user, true);
                    this.setOnline(true);
                }
            }
        }
    }

    /**
     * prints selection menu for toolcards
     * @throws RemoteException  if the reference could not be accessed
     */
    private void viewUseToolCard() throws RemoteException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            PrintUseToolCard.print((GameModel) gameModel, gameModel.getActualPlayer().getToolCardSelected(), choices);
            network.update(this);
        }
    }

    /**
     * prints selection menu for toolcards
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewUseToolCard2() throws IOException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            PrintUseToolCard2.print((GameModel) gameModel, gameModel.getActualPlayer().getToolCardSelected(), choices);
            if(socketConnection){
                setDeleteConnectionSocket(true);
                ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                ob.writeObject(this);
            }
            else
                network.update(this);
        }
    }

    /**
     * prints selection menu for toolcards
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewUseToolCard3() throws IOException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            PrintUseToolCard3.print((GameModel) gameModel, gameModel.getActualPlayer().getToolCardSelected(), choices);
            if(socketConnection){
                setDeleteConnectionSocket(true);
                ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                ob.writeObject(this);
            }
            else
                network.update(this);
        }
    }

    /**
     * print error message
     * @throws RemoteException if the reference could not be accessed
     */
    private void viewError() throws IOException {
        if(user.equals(gameModel.getActualPlayer().getUsername())) {
            System.out.println("PLEASE DO IT AGAIN CORRECTLY!");
            if(socketConnection){
                setDeleteConnectionSocket(true);
                ObjectOutputStream ob = new ObjectOutputStream(socket.getOutputStream());
                ob.writeObject(this);
            }
            else
                network.update(this);
        }
    }

    /**
     * print a message
     * @param s the message to be printed
     */
   @Override
   public void print(String s){
       System.out.println(s);
   }

    /**
     * prints an error message
     * @param error the error message to be printed
     * @throws RemoteException if the reference could not be accessed
     */
   @Override
   public void printError(String error) throws RemoteException {
       if(user.equals(gameModel.getActualPlayer().getUsername())) {
           System.out.println(error);
       }
   }

    /**
     *
     * @param gameModel the gamemodel of the match
     * @throws RemoteException if the reference could not be accessed
     */
   @Override
   public void update(RemoteGameModel gameModel) throws RemoteException {
       this.gameModel = gameModel;
       try {
           this.run();
       } catch (IOException e) {
           //do nothing
       }
   }

    public void updateSocket() throws IOException, ClassNotFoundException {
        while(!endGame) {
            if(!getDeleteConnectionSocket()) {
                ObjectInputStream ob = new ObjectInputStream(socket.getInputStream());
                this.gameModel = (RemoteGameModel) ob.readObject();
                this.run();
            }
        }
    }

}
