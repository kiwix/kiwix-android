import os 

def get_all_language_codes():

	# Get the path of the res folder
	res_path = os.path.join(os.getcwd(),'res')

	# Get all the ISO 639-1 language codes from the suffix of the value folders
	files = [f.split('values-')[1] for f in os.listdir(res_path) if f.startswith('values-')]

	# Append the English Locale to the list, since the default values folder, (the english) values folder
	# does not have a suffix and gets ignored when creating the above list
	files.append('en')

	return files

def write_locales(locales):

	# Create a CSV file with all the langauge codes in the assets folder
	with open(os.path.join(os.getcwd(), 'assets', 'locales.txt'), 'w') as f:
		f.write(',\n'.join(locales))


if __name__ == '__main__':
	write_locales(get_all_language_codes())

