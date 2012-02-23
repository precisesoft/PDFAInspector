# Author: Prateek Arora
# including required modules
from django.shortcuts import *
from settings import *
from django.contrib.auth.decorators import login_required
import os, re
from forms import userChangeForm
import json

# profile homepage
@login_required
def profile(request):
	currentPage = "userprofile"
	username = request.user.username
	first_name = request.user.first_name
	last_name = request.user.last_name
	email = request.user.email
	last_login = request.user.last_login
	date_joined = request.user.date_joined
	return render_to_response("userprofile/profile.html", locals())

# manage reports
@login_required
def managereports(request):
	currentPage = "userprofile"
	user = request.user
	file_list = os.listdir(user.get_profile().filepath)
	regex = re.compile('json-.*')
	print file_list
	json_file_list = []
	for file in file_list:
		if regex.match(file):
			json_file_list.append(file)
	return render_to_response("userprofile/reports.html", locals())

# manage rules
@login_required
def managerules(request):
	currentPage = "userprofile"
	return render_to_response("userprofile/rules.html", locals())
	
@login_required
def change(request):
	if (request.method=="POST"):
		form = userChangeForm(request.POST)
		if form.is_valid():
			request.user.first_name = form.cleaned_data['first_name']
			request.user.last_name = form.cleaned_data['last_name']
			request.user.email = form.cleaned_data['email']
			request.user.save()
			return HttpResponseRedirect('/accounts/profile/')
	else:
		form = userChangeForm()
		return render_to_response("userprofile/change.html", locals(), context_instance=RequestContext(request))