package app.zophop.mvibase

interface ChaloBasePartialChange<ViewState> {
    fun reduce(oldState: ViewState): ViewState {
        return oldState
    }
}
