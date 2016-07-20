
var utils = require('cordova/utils'),
  exec = require('cordova/exec'),
  cordova = require('cordova');

function Nslookup (ipList) {
  this.results = null;
}

Nslookup.prototype.nslookup = function (query, success, err) {
  var successCallback, errorCallback, self;
  self = this;
  successCallback = function (r) {
    self.results = r;
    if (success && typeof success === 'function') {
      success(r);
    }
  };
  errorCallback = function (e) {
    utils.alert('[ERROR] Error initializing Cordova: ' + e);
    if (err && typeof err === 'function') {
      err(e);
    }
  };
  exec(successCallback, errorCallback, "Nslookup", "getNsLookupInfo", query);
};

module.exports = Nslookup;
