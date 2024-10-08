from datetime import datetime
import sqlite3
from sys import argv
from flask import Flask, jsonify, make_response, request, g


# This is an example REST API that would show you how
app = Flask(__name__)


# Global variables, change as needed
DATABASE = 'supermarket.db'     # Name of database file
PORT = 5000                     # Port to host API on



# Because Flask will open sockets across several threads, it causes issues trying to access the database
# The database only like to be accessed on the same thread the connection was made from, and doesn't like
# making several connections across several threads. The solution is to store an instance of the connection
# in a global variable, then the connection only exists on the main thread.
def get_db():
    db = getattr(g, '_database', None)
    if db is None:
        db = g._database = sqlite3.connect(DATABASE, isolation_level=None)

    # This makes interacting with results from queries very pleasant
    # Under the hood, these Row objects are namedtuples, so we can access them by index or by key
    db.row_factory = sqlite3.Row
    return db


# Cleanup function, called automatically by Flask when the app is closed.
@app.teardown_appcontext
def close_connection(exception):
    db = getattr(g, '_database', None)
    if db is not None:
        db.close()


# Makes life easy and prevents SQL injections
# Takes a parameterized SQL query, and a tuple of said parameters
# Returns the first row if there's a result
def query_db(query, args=(), one=False):
    cur = get_db().execute(query, args)
    rv = cur.fetchall()
    cur.close()
    return (rv[0] if rv else None) if one else rv


# Just to demonstrate how a scanner might usually interface with the API
# In a real world system, this would carry out an SQL query build a response from contents of the result
@app.route("/get-details/<string:barcode>/")
def getDetails(barcode):
    content = {"long-name": "Supermarket Own Brand Tomato Soup",
               "short-name ": "OB Tomato Soup",
               "gross-weight": 1000000,
               "weight-units": "milligrams",
               "price": 350,
               "age-restricted": False}
    jsonResponse = make_response((jsonify(content), 200))
    return jsonResponse


# Put a nonce for a discount into the database
@app.route('/submit-nonce',methods = ['PUT'])
def submitNonce():
    nonce = request.values["nonce"]
    expiry = request.values["expires"]
    keyID = request.values["key-id"]
    query_db("INSERT INTO discounts (nonce, expiration, key_id) VALUES (?, ?, ?)", (nonce, int(expiry), keyID))
    return "Successfully added new nonce", 200


# Check a discount nonce is valid, without actually destroying the nonce
# This is good for when you add items to the basket, and want to check they're valid, before checking out
@app.route("/verify-nonce/<string:nonce>/")
def verifyNonce(nonce):
    rows = query_db("SELECT * FROM discounts WHERE nonce = ?", (nonce,))
    if len(rows) == 0:
        return "Invalid Nonce", 410
    expiryTimestamp = rows[0]["expiration"]
    if datetime.now().timestamp() > expiryTimestamp:
        return "Invalid Nonce", 410
    return f"Valid Nonce", 200


# Deletes a nonce from the database, invalidating it permanently
# This is good for checking out and finalising a transaction, when you know the discount shouldn't be used again.
@app.route('/delete-nonce/<string:nonce>',methods = ['DELETE'])
def consumeNonce(nonce):
    rows = query_db("DELETE FROM discounts WHERE NONCE = ?", (nonce,))
    return "Nonce invalidated", 200


def main():
    with app.app_context():
        db = get_db()
        # with app.open_resource('schema.sql', mode='r') as f:
        #     db.cursor().executescript(f.read())
        db.cursor().executescript("CREATE TABLE IF NOT EXISTS discounts (nonce TEXT PRIMARY KEY, expiration INTEGER, key_id TEXT)")
        db.commit()
        app.run(port=PORT, use_reloader=False)


if __name__ == "__main__":
    if len(argv) != 2:
        print("Program Usage: python API.py port_number")
    else:
        PORT = argv[1]
        main()