" Remove whitespace from end of lines when saving
autocmd BufWritePre * :%s/\s\+$//e

" Use spaces in XML files
autocmd Filetype xml set expandtab
autocmd Filetype kotlin set expandtab

" 4-width tabs by default
set noexpandtab
set shiftwidth=4
set tabstop=4
